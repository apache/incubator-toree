package com.ibm.spark.kernel.protocol.v5.interpreter.tasks

import akka.actor.{Props, Actor}
import com.ibm.spark.interpreter.{ExecuteError, Interpreter}
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
          sender ! Left(output)
        case IR.Error =>
          val error = result.right.get
          sender ! Right(error)
        case _ =>
          sender ! Right(
            ExecuteError("Unknown Error", "Unable to identify error!", List()))
      }
    case _ =>
      sender ! "Unknown message" // TODO: Provide a failure message type to be passed around?
  }
}
