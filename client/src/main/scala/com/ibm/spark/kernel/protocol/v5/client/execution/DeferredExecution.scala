package com.ibm.spark.kernel.protocol.v5.client.execution

import com.ibm.spark.kernel.protocol.v5.content.{ExecuteReply, ExecuteReplyError, ExecuteResult, StreamContent}

case class DeferredExecution() {
  var executeResultCallbacks: List[(ExecuteResult) => Unit] = Nil
  var streamCallbacks: List[(StreamContent) => Unit] = Nil
  var errorCallbacks: List[(ExecuteReplyError) => Unit] = Nil

  var executeResultOption: Option[ExecuteResult] = None
  var executeReplyOption: Option[ExecuteReply] = None

  /**
   * Registers a callback for handling ExecuteResult messages.
   * This {@param callback} will run once on successful code execution and
   * then be unregistered. If {@param callback} is registered after the result
   * has been returned it will be invoked immediately.
   * In the event of a failure {@param callback} will never be called.
   * @param callback
   * @return
   */
  def onResult(callback: (ExecuteResult) => Unit): DeferredExecution = {
    this.executeResultCallbacks = callback :: this.executeResultCallbacks
    processCallbacks()
    this
  }

  def onStream(callback: (StreamContent)=>Unit): DeferredExecution = {
    this.streamCallbacks = callback :: this.streamCallbacks
    this
  }

  def onError(callback: (ExecuteReplyError)=>Unit): DeferredExecution = {
    this.errorCallbacks = callback :: this.errorCallbacks
    processCallbacks()
    this
  }

  def processCallbacks(): Unit = {
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
          // This prevents methods from getting called again when
          // a callback is registered after processing occurs
          this.executeResultCallbacks = Nil
      case _ => // TODO log a message
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
