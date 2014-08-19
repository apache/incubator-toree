package com.ibm.spark.client

import akka.pattern.ask
import akka.util.Timeout
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.ExecuteRequest
import com.ibm.spark.kernel.protocol.v5.socket._
import com.ibm.spark.utils.LogLike

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * Client API for Spark Kernel.
 *
 * Note: This takes a moment to initialize an actor system, so take appropriate action
 * if an API is to be used immediately after initialization.
 */
class SparkKernelClient(actorLoader: ActorLoader) extends LogLike {
  implicit val timeout = Timeout(1.minutes)

  def submit(code: String): Future[Any] = {
    val request = ExecuteRequest(code, false, true, UserExpressions(), true)
    actorLoader.load(MessageType.ExecuteRequest) ? request
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