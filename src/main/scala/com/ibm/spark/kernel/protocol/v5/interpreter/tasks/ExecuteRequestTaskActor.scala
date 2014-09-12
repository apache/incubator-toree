package com.ibm.spark.kernel.protocol.v5.interpreter.tasks

import java.io.OutputStream

import akka.actor.{Props, Actor}
import com.ibm.spark.interpreter.{ExecuteAborted, Results, ExecuteError, Interpreter}
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content._

object ExecuteRequestTaskActor {
  def props(interpreter: Interpreter): Props =
    Props(classOf[ExecuteRequestTaskActor], interpreter)
}

class ExecuteRequestTaskActor(interpreter: Interpreter) extends Actor {
  require(interpreter != null)

  override def receive: Receive = {
    case (executeRequest: ExecuteRequest, outputStream: OutputStream) =>
      interpreter.updatePrintStreams(System.in, outputStream, outputStream)
      val (success, result) = interpreter.interpret(executeRequest.code)
      success match {
        case Results.Success =>
          val output = result.left.get
          sender ! Left(output)
        case Results.Error =>
          val error = result.right.get
          sender ! Right(error)
        case Results.Aborted =>
          sender ! Right(new ExecuteAborted)
        case Results.Incomplete =>
          // This just sends an ExecuteReplyOk for cells with no
          // executable code
          sender ! Left("")
      }
    case _ =>
      sender ! "Unknown message" // TODO: Provide a failure message type to be passed around?
  }
}
