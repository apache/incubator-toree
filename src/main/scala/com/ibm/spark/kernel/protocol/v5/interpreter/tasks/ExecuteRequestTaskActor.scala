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
      val (success, result) = interpreter.interpret(executeRequest.code)
      success match {
        case IR.Success =>
          val output = result.left.get
          sender ! (
            ExecuteReplyOk(1, Some(Payloads()), Some(UserExpressions())),
            ExecuteResult(1, Data("text/plain" -> output), Metadata())
          )
        case IR.Error =>
          val error = result.right.get
          sender ! (
            ExecuteReplyError(
              1, Some(error.name), Some(error.value), Some(error.stackTrace)
            ),
            // TODO: Investigate what should be returned on an error
            // NOTE: Have not found anyone supporting v5.0 protocol
            // NOTE: Should follow someone that has implemented v5.0
            ExecuteResult(1, Data("text/plain" -> error.toString), Metadata())
          )
        case _ =>
          sender ! (
            ExecuteReplyError(
              1, Some("Incomplete"), Some("More input needed!"), Some(List())
            ),
            ExecuteResult(1, Data("text/plain" -> ""), Metadata())
          )
      }
    case _ =>
      sender ! "Unknown message" // TODO: Provide a failure message type to be passed around?
  }
}
