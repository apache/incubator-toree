package com.ibm.spark.kernel.protocol.v5.interpreter.tasks

import akka.actor.{Actor, Props}
import com.ibm.spark.interpreter.Interpreter
import com.ibm.spark.kernel.protocol.v5.content.CompleteRequest
import com.ibm.spark.utils.LogLike

object CodeCompleteTaskActor {
  def props(interpreter: Interpreter): Props =
    Props(classOf[CodeCompleteTaskActor], interpreter)
}

class CodeCompleteTaskActor(interpreter: Interpreter)
  extends Actor with LogLike {
  require(interpreter != null)

  override def receive: Receive = {
    case completeRequest: CompleteRequest =>
      logger.debug("Invoking the interpreter completion")
      sender ! interpreter.completion(completeRequest.code, completeRequest.cursor_pos)
    case _ =>
      sender ! "Unknown message" // TODO: Provide a failure message type to be passed around?
  }
}
