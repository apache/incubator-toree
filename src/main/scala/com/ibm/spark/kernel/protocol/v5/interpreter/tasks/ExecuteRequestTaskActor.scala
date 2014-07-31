package com.ibm.spark.kernel.protocol.v5.interpreter.tasks

import akka.actor.{Props, Actor}
import com.ibm.spark.interpreter.Interpreter
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content._

import scala.tools.nsc.interpreter._

object ExecuteRequestTaskActor {
  def props(interpreter: Interpreter): Props =
    Props(classOf[ExecuteRequestTaskActor], interpreter)
}

class ExecuteRequestTaskActor(interpreter: Interpreter) extends Actor {
  require(interpreter != null)

  override def receive: Receive = {
    case executeRequest: ExecuteRequest =>
      val success = interpreter.interpret(executeRequest.code)
      success match {
        case IR.Success =>
          sender ! ExecuteReplyOk(
            1,
            Some(Payloads()),
            Some(UserExpressions()))
        case IR.Error =>
          sender ! ExecuteReplyError(
            1,
            Some("NAME"),
            Some("VALUE"),
            Some(List()))
        case _ =>
          sender ! ExecuteReplyError(
            1,
            Some("Incomplete"),
            Some("More input needed!"),
            Some(List()))
      }
    case _ =>
      sender ! "Unknown message" // TODO: Provide a failure message type to be passed around?
  }
}
