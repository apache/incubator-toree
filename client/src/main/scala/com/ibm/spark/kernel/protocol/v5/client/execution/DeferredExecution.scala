/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.spark.kernel.protocol.v5.client.execution

import com.ibm.spark.kernel.protocol.v5.content.{ExecuteReply, ExecuteReplyError, ExecuteResult, StreamContent}
import com.ibm.spark.utils.LogLike

case class DeferredExecution() extends LogLike {
  private var executeResultCallbacks: List[(ExecuteResult) => Unit] = Nil
  private var streamCallbacks: List[(StreamContent) => Unit] = Nil
  private var errorCallbacks: List[(ExecuteReplyError) => Unit] = Nil
  private var completionCallbacks: List[() => Unit] = Nil
  private var executeResultOption: Option[ExecuteResult] = None
  private var executeReplyOption: Option[ExecuteReply] = None

  /**
   * Registers a callback for handling ExecuteResult messages.
   * This {@param callback} will run once on successful code execution and
   * then be unregistered. If {@param callback} is registered after the result
   * has been returned it will be invoked immediately.
   * In the event of a failure {@param callback} will never be called.
   * @param callback A callback function, which will be invoked at most once,
   *                 with an ExecuteResult IPython message
   * @return  The DeferredExecution with the given callback registered.
   */
  def onResult(callback: (ExecuteResult) => Unit): DeferredExecution = {
    this.executeResultCallbacks = callback :: this.executeResultCallbacks
    processCallbacks()
    this
  }

  /**
   * Registers a callback for handling StreamContent messages.
   * Ths {@param callback} can be called 0 or more times. If the
   * {@param callback} is registered after StreamContent messages have been
   * emitted, the {@param callback} will only receive messages emitted after the
   * point of registration.
   * @param callback A callback function, which can be invoked 0 or more times,
   *                 with Stream Ipython messages
   * @return  The DeferredExecution with the given callback registered.
   */
  def onStream(callback: (StreamContent) => Unit): DeferredExecution = {
    this.streamCallbacks = callback :: this.streamCallbacks
    this
  }

  /**
   * Registers a callback for handling ExecuteReply messages when there is an
   * error during code execution. This {@param callback} will run once on failed
   * code execution and then be unregistered. If {@param callback} is registered
   * after the error reply has been returned it will be invoked immediately.
   * In the event of successful code execution {@param callback} will never be
   * called.
   * @param callback A callback function, which will be invoked at most once,
   *                 with an ExecuteReply IPython message
   * @return  The DeferredExecution with the given callback registered.
   */
  def onError(callback: (ExecuteReplyError) => Unit): DeferredExecution = {
    this.errorCallbacks = callback :: this.errorCallbacks
    processCallbacks()
    this
  }

  /**
   * Registers a callback to be notified when code completion has completed
   * successfully. {@param callback} will not be called if an error has been
   * encountered, use {@method onError}.
   * @param callback The callback to register.
   * @return This deferred execution
   */
  def onSuccessfulCompletion(callback: () => Unit): DeferredExecution = {
    this.completionCallbacks = callback :: this.completionCallbacks
    processCallbacks()
    this
  }

  private def processCallbacks(): Unit = {
    (executeReplyOption, executeResultOption) match {
      case (Some(executeReply), Some(executeResult)) if executeReply.status.equals("error") =>
          // call error callbacks
          this.errorCallbacks.foreach(_(executeReply))
          // This prevents methods from getting called again when
          // a callback is registered after processing occurs
          this.errorCallbacks = Nil
      case (Some(executeReply), Some(executeResult)) if executeReply.status.equals("ok") =>
          // call result callbacks
          this.executeResultCallbacks.foreach(_(executeResult))
          // call the completion callbacks
          this.completionCallbacks.foreach(_())
          // This prevents methods from getting called again when
          // a callback is registered after processing occurs
          this.executeResultCallbacks = Nil
      case _ =>
        logger.debug(
          s"""
              Did not invoke client callbacks.
              ExecuteReply was: ${executeReplyOption}
              ExecuteResult was: ${executeResultOption}
           """.stripMargin)
    }
  }

  def resolveResult(executeResultMessage: ExecuteResult): Unit = {
    this.executeResultOption = Some(executeResultMessage)
    processCallbacks()
  }

  def resolveReply(executeReplyMessage: ExecuteReply): Unit = {
    this.executeReplyOption = Some(executeReplyMessage)
    processCallbacks()
  }

  def emitStreamContent(streamContent: StreamContent): Unit = {
    this.streamCallbacks.foreach(streamCallback => {
      streamCallback(streamContent)
    })
  }
}
