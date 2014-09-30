package com.ibm.spark.kernel.protocol.v5.interpreter.tasks

import java.io.OutputStream

import akka.actor.{Props, Actor}
import com.ibm.spark.interpreter.{ExecuteAborted, Results, ExecuteError, Interpreter}
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content._
import com.ibm.spark.utils.LogLike

object ExecuteRequestTaskActor {
  def props(interpreter: Interpreter): Props =
    Props(classOf[ExecuteRequestTaskActor], interpreter)
}

class ExecuteRequestTaskActor(interpreter: Interpreter) extends Actor with LogLike {
  require(interpreter != null)

  override def receive: Receive = {
    case (executeRequest: ExecuteRequest, outputStream: OutputStream) =>
      // If the cell is not empty, then interpret.
      if(executeRequest.code.trim != "") {
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
            // If we get an incomplete it's most likely a syntax error, so
            // let the user know.
            sender ! Right(new ExecuteError("Syntax Error.", "", List()))
        }
      }
      // If we get empty code from a cell then just return ExecuteReplyOk
      else {
        sender ! Left("")
      }
    case _ =>
      sender ! "Unknown message" // TODO: Provide a failure message type to be passed around?
  }
}
