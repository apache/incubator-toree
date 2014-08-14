package com.ibm.spark.client

import akka.pattern.ask
import akka.util.Timeout
import com.ibm.spark.client.SparkKernelClient.resolveShell
import com.ibm.spark.client.exception.ShellException
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.{ExecuteReply, ExecuteRequest, ExecuteResult}
import com.ibm.spark.kernel.protocol.v5.socket._
import com.ibm.spark.utils.LogLike

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object SparkKernelClient extends LogLike {
  private[client] def resolveShell(result: Try[Any], replyCallback: ExecuteReply => Unit, errCallback: ShellException => Unit): Unit = {
    result match {
      // standard Shell reply
      case Success(message: ExecuteReply) =>
        replyCallback(message)

      // this should never be matched
      case Success(message) =>
        logger.debug("Received unhandled message from Shell: \n" + message)

      case Failure(errMessage) =>
        errCallback(new ShellException(errMessage))
    }
  }
}

/**
 * Client API for Spark Kernel.
 *
 * Note: This takes a moment to initialize an actor system, so take appropriate action
 * if an API is to be used immediately after initialization.
 */
class SparkKernelClient(actorLoader: ActorLoader) extends LogLike {
  implicit val timeout = Timeout(1.minutes)

  // user should provide success/failure lambdas that act on status of the
  // ExecuteReply returned from the ask
  def execute(code: String, replyCallback: ExecuteReply => Unit, resultCallback: ExecuteResult => Unit, errCallback: ShellException => Unit):
    Unit = {

      val request = ExecuteRequest(code, false, true, UserExpressions(), true)
      val requestTuple = ExecuteRequestTuple(request, resultCallback, errCallback)
      val future = actorLoader.load(MessageType.ExecuteRequest) ? requestTuple

      future.onComplete {
        case message =>
          resolveShell(message, replyCallback, errCallback)
      }
  }

  // TODO: hide this? just heartbeat to see if kernel is reachable?
  def heartbeat(failure: () => Unit): Unit = {
    val future = actorLoader.load(SocketType.Heartbeat) ? HeartbeatMessage

    future.onComplete {
      case Success(_) =>
        logger.info("Client received heartbeat.")
      case Failure(_) =>
        failure()
        logger.info("There was an error receiving heartbeat from kernel.")
    }
  }
}