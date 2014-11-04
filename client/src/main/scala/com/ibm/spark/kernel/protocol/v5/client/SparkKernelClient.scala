package com.ibm.spark.kernel.protocol.v5.client

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.ExecuteRequest
import com.ibm.spark.kernel.protocol.v5.socket._
import com.ibm.spark.utils.LogLike
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
/**
 * Client API for Spark Kernel.
 *
 * Note: This takes a moment to initialize an actor system, so take appropriate action
 * if an API is to be used immediately after initialization.
 *
 * The actorSystem parameter allows shutdown of this client's ActorSystem.
 */
class SparkKernelClient(actorLoader: ActorLoader, actorSystem: ActorSystem) extends LogLike {
  implicit val timeout = Timeout(21474835.seconds)

  /**
   * Executed Scala code on the Spark Kernel.
   * @param code Scala code to execute
   * @return A Future containing the result of the execution.
   */
  def submit(code: String): Future[Any] = {
    val request = ExecuteRequest(code, false, true, UserExpressions(), true)
    actorLoader.load(MessageType.ExecuteRequest) ? request
  }

  /**
   * Execute streaming code on the Spark Kernel.
   * @param code Streaming Scala code (prints to stdout)
   * @param callback Function called when stream messages are received
   */
  def stream(code: String, callback: Any => Unit): Future[Any] = {
    val request = ExecuteRequest(code, false, true, UserExpressions(), true)
    actorLoader.load(MessageType.ExecuteRequest) ? ExecuteRequestTuple(request, callback)
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

  def shutdown() = {
    logger.info("Shutting down client")
    actorSystem.shutdown()
  }
}