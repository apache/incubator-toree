package com.ibm.interpreter

import akka.actor.Actor
import com.ibm.kernel.protocol.v5._
import com.ibm.kernel.protocol.v5.content._

import scala.tools.nsc.interpreter._

class InterpreterActor(interpreter: ScalaInterpreter) extends Actor {
  require(interpreter != null)

  interpreter.start()

  override def receive: Receive = {
    case executeRequest: ExecuteRequest =>
      val success = interpreter.interpret(executeRequest.code)
      success match {
        case IR.Success =>
          sender ! ExecuteReplyOk(1, Some(Payloads()), Some(UserExpressions()))
        case IR.Error =>
          sender ! ExecuteReplyError(1, Some("NAME"), Some("VALUE"),
            Some(List()))
        case IR.Incomplete =>
          sender ! ExecuteReplyError(1, Some("Incomplete"), Some("More input needed!"), Some(List()))
      }
    case _ =>
      sender ! "Unknown message" // TODO: Provide a failure message type to be passed around?
  }
}
