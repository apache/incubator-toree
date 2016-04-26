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
import org.apache.toree.kernel.protocol.v5._

import scala.util.Try

@Experimental
object CommCallbacks {
  type OpenCallback = (CommWriter, UUID, String, MsgData) => Unit
  type MsgCallback = (CommWriter, UUID, MsgData) => Unit
  type CloseCallback = (CommWriter, UUID, MsgData) => Unit
}

import org.apache.toree.comm.CommCallbacks._

/**
 * Represents available callbacks to be triggered when various Comm events
 * are triggered.
 *
 * @param openCallbacks The sequence of open callbacks
 * @param msgCallbacks The sequence of msg callbacks
 * @param closeCallbacks The sequence of close callbacks
 */
@Experimental
class CommCallbacks(
  val openCallbacks: Seq[CommCallbacks.OpenCallback] = Nil,
  val msgCallbacks: Seq[CommCallbacks.MsgCallback] = Nil,
  val closeCallbacks: Seq[CommCallbacks.CloseCallback] = Nil
) {

  /**
   * Adds a new open callback to be triggered.
   *
   * @param openCallback The open callback to add
   *
   * @return The updated CommCallbacks instance
   */
  def addOpenCallback(openCallback: OpenCallback): CommCallbacks =
    new CommCallbacks(
      openCallbacks :+ openCallback,
      msgCallbacks,
      closeCallbacks
    )

  /**
   * Adds a new msg callback to be triggered.
   *
   * @param msgCallback The msg callback to add
   *
   * @return The updated CommCallbacks instance
   */
  def addMsgCallback(msgCallback: MsgCallback): CommCallbacks =
    new CommCallbacks(
      openCallbacks,
      msgCallbacks :+ msgCallback,
      closeCallbacks
    )

  /**
   * Adds a new close callback to be triggered.
   *
   * @param closeCallback The close callback to add
   *
   * @return The updated CommCallbacks instance
   */
  def addCloseCallback(closeCallback: CloseCallback): CommCallbacks =
    new CommCallbacks(
      openCallbacks,
      msgCallbacks,
      closeCallbacks :+ closeCallback
    )

  /**
   * Removes the specified open callback from the collection of callbacks.
   *
   * @param openCallback The open callback to remove
   *
   * @return The updated CommCallbacks instance
   */
  def removeOpenCallback(openCallback: OpenCallback): CommCallbacks =
    new CommCallbacks(
      openCallbacks.filterNot(_ == openCallback),
      msgCallbacks,
      closeCallbacks
    )

  /**
   * Removes the specified msg callback from the collection of callbacks.
   *
   * @param msgCallback The msg callback to remove
   *
   * @return The updated CommCallbacks instance
   */
  def removeMsgCallback(msgCallback: MsgCallback): CommCallbacks =
    new CommCallbacks(
      openCallbacks,
      msgCallbacks.filterNot(_ == msgCallback),
      closeCallbacks
    )

  /**
   * Removes the specified close callback from the collection of callbacks.
   *
   * @param closeCallback The close callback to remove
   *
   * @return The updated CommCallbacks instance
   */
  def removeCloseCallback(closeCallback: CloseCallback): CommCallbacks =
    new CommCallbacks(
      openCallbacks,
      msgCallbacks,
      closeCallbacks.filterNot(_ == closeCallback)
    )

  /**
   * Executes all registered open callbacks and returns a sequence of results.
   *
   * @param commWriter The Comm Writer that can be used for responses
   * @param commId The Comm Id to pass to all open callbacks
   * @param targetName The Comm Target Name to pass to all open callbacks
   * @param data The data to pass to all open callbacks
   *
   * @return The sequence of results from trying to execute callbacks
   */
  def executeOpenCallbacks(
    commWriter: CommWriter, commId: UUID, targetName: String, data: MsgData
  ) = openCallbacks.map(f => Try(f(commWriter, commId, targetName, data)))

  /**
   * Executes all registered msg callbacks and returns a sequence of results.
   *
   * @param commWriter The Comm Writer that can be used for responses
   * @param commId The Comm Id to pass to all msg callbacks
   * @param data The data to pass to all msg callbacks
   *
   * @return The sequence of results from trying to execute callbacks
   */
  def executeMsgCallbacks(commWriter: CommWriter, commId: UUID, data: MsgData) =
    msgCallbacks.map(f => Try(f(commWriter, commId, data)))

  /**
   * Executes all registered close callbacks and returns a sequence of results.
   *
   * @param commWriter The Comm Writer that can be used for responses
   * @param commId The Comm Id to pass to all close callbacks
   * @param data The data to pass to all close callbacks
   *
   * @return The sequence of results from trying to execute callbacks
   */
  def executeCloseCallbacks(commWriter: CommWriter, commId: UUID, data: MsgData) =
    closeCallbacks.map(f => Try(f(commWriter, commId, data)))
}
