/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package org.apache.toree.comm

import java.util.UUID

import org.apache.toree.annotations.Experimental
import org.apache.toree.comm.CommCallbacks.{CloseCallback, OpenCallback}
import org.apache.toree.kernel.protocol.v5
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content.CommContent

/**
 * Represents a manager for Comm connections that facilitates and maintains
 * connections started and received through this service.
 *
 * @param commRegistrar The registrar to use for callback registration
 */
@Experimental
abstract class CommManager(private val commRegistrar: CommRegistrar) {
  /**
   * The base function to call that performs a link given the target name and
   * the Comm id for the specific instance.
   */
  private val linkFunc: OpenCallback =
    (_, commId, targetName, _) => commRegistrar.link(targetName, commId)

  /**
   * The base function to call that performs an unlink given the Comm id for
   * the specific instance.
   */
  private val unlinkFunc: CloseCallback =
    (_, commId, _) => commRegistrar.unlink(commId)

  // TODO: This is potentially bad design considering appending methods to
  //       CommWriter will not require this class to be updated!
  /**
   * Represents a wrapper for a CommWriter instance that links and unlinks
   * when invoked.
   *
   * @param commWriter The CommWriter instance to wrap
   */
  private class WrapperCommWriter(private val commWriter: CommWriter)
    extends CommWriter(commWriter.commId)
  {
    override protected[comm] def sendCommKernelMessage[
      T <: KernelMessageContent with CommContent
    ](commContent: T): Unit = commWriter.sendCommKernelMessage(commContent)

    // Overridden to unlink before sending close message
    override def writeClose(data: MsgData): Unit = {
      unlinkFunc(this, this.commId, data)
      commWriter.writeClose(data)
    }

    // Overridden to unlink before sending close message
    override def close(): Unit = {
      unlinkFunc(this, this.commId, null)
      commWriter.close()
    }

    // Overridden to link before sending open message
    override def writeOpen(targetName: String, data: MsgData): Unit = {
      linkFunc(this, this.commId, targetName, data)
      commWriter.writeOpen(targetName, data)
    }

    override def writeMsg(data: MsgData): Unit = commWriter.writeMsg(data)
    override def write(cbuf: Array[Char], off: Int, len: Int): Unit =
      commWriter.write(cbuf, off, len)
    override def flush(): Unit = commWriter.flush()
  }

  /**
   * Loads the specified target and provides a registrar pointing to the target.
   *
   * @param targetName The name of the target to load
   *
   * @return The CommRegistrar pointing to the target
   */
  def withTarget(targetName: String):  CommRegistrar = {
    commRegistrar.withTarget(targetName)
  }

  /**
   * Registers a new Comm for use on the kernel. Establishes default callbacks
   * to link and unlink specific Comm instances for the new target.
   *
   * @param targetName The name of the target to register
   *
   * @return The new CommRegistrar set to the provided target
   */
  def register(targetName: String): CommRegistrar = {
    commRegistrar.register(targetName)
      .addOpenHandler(linkFunc)
      .addCloseHandler(unlinkFunc)
  }

  /**
   * Unregisters the specified target used for Comm messages.
   *
   * @param targetName The name of the target to unregister
   *
   * @return Some collection of callbacks associated with the target if it was
   *         registered, otherwise None
   */
  def unregister(targetName: String): Option[CommCallbacks] = {
    commRegistrar.unregister(targetName)
  }

  /**
   * Indicates whether or not the specified target is currently registered with
   * this Comm manager.
   *
   * @param targetName The name of the target
   *
   * @return True if the target is registered, otherwise false
   */
  def isRegistered(targetName: String): Boolean =
    commRegistrar.isRegistered(targetName)

  /**
   * Opens a new Comm connection. Establishes a new link between the specified
   * target and the generated Comm id.
   *
   * @param targetName The name of the target to connect
   * @param data The optional data to send
   *
   * @return The new CommWriter representing the connection
   */
  def open(targetName: String, data: v5.MsgData = v5.MsgData.Empty): CommWriter = {
    val commId = UUID.randomUUID().toString

    // Create our CommWriter and wrap it to establish links and unlink on close
    val commWriter = new WrapperCommWriter(newCommWriter(commId))

    // Establish the actual connection
    commWriter.writeOpen(targetName, data)

    commWriter
  }

  /**
   * Creates a new CommWriter instance given the Comm id.
   *
   * @param commId The Comm id to use with the Comm writer
   *
   * @return The new CommWriter instance
   */
  protected def newCommWriter(commId: v5.UUID): CommWriter
}
