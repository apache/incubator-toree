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

import org.apache.toree.annotations.Experimental
import org.apache.toree.kernel.protocol.v5
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content._

import java.io.Writer

/**
 * Represents a basic writer used to communicate comm-related messages.
 *
 * @param commId The comm id associated with this writer (defaults to a
 *               random UUID)
 */
@Experimental
abstract class CommWriter(
  private[comm] val commId: UUID = java.util.UUID.randomUUID().toString
) extends Writer {

  private val MessageFieldName = "message"

  /**
   * Packages and sends an open message with the provided target and data.
   *
   * @param targetName The name of the target (used by the recipient)
   * @param data The optional data to send with the open message
   */
  def writeOpen(targetName: String, data: MsgData = MsgData.Empty) =
    sendCommKernelMessage(CommOpen(commId, targetName, data))

  /**
   * Packages and sends a message with the provided data.
   *
   * @param data The data to send
   */
  def writeMsg(data: v5.MsgData) = {
    require(data != null)

    sendCommKernelMessage(CommMsg(commId, data))
  }

  /**
   * Packages and sends a close message with the provided data.
   *
   * @param data The optional data to send with the close message
   */
  def writeClose(data: v5.MsgData = MsgData.Empty) =
    sendCommKernelMessage(CommClose(commId, data))

  /**
   * Writes the data as a new message, wrapped with a "message" JSON field.
   *
   * @param cbuf The array of characters to send
   * @param off The offset (0 is the start) of the array to use
   * @param len The number of characters to send
   */
  override def write(cbuf: Array[Char], off: Int, len: Int): Unit = {
    val message = new String(cbuf.slice(off, len))
    val packedMessage = packageMessage(message)
    sendCommKernelMessage(packedMessage)
  }

  /**
   * Does nothing in this implementation.
   */
  override def flush(): Unit = {}

  /**
   * Sends a close message without any data.
   */
  override def close(): Unit = writeClose()

  /**
   * Packages a string message as a Comm message.
   *
   * @param message The string message to package
   *
   * @return The resulting CommMsg wrapper
   */
  private def packageMessage(message: String) =
    CommMsg(commId, MsgData(MessageFieldName -> message))

  /**
   * Sends the comm message (open/msg/close) to the actor responsible for
   * relaying messages.
   *
   * @param commContent The message to relay (will be packaged)
   *
   * @tparam T Either CommOpen, CommMsg, or CommClose
   */
  protected[comm] def sendCommKernelMessage[
    T <: KernelMessageContent with CommContent
  ](commContent: T)
}
