package com.ibm.spark.client

import java.io.File

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.ibm.spark.kernel.protocol.v5.MessageType._
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.ExecuteRequest
import com.ibm.spark.kernel.protocol.v5.socket._
import com.ibm.spark.utils.LogLike

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * Client API for Spark Kernel.
 *
 * Note: This takes a moment to initialize an actor system, so take appropriate action
 * if an API is to be used immediately after initialization.
 */
class SparkKernelClient extends LogLike { // todo instantiate with signature
  implicit val timeout = Timeout(1.minute)

  val actorSystem = ActorSystem("spark-client-actor-system")
  val actorLoader = SimpleActorLoader(actorSystem)

  val profile = Option(new File("src/main/resources/profile.json"))
  val socketConfigReader = new SocketConfigReader(profile)

  val socketFactory = new SocketFactory(socketConfigReader.getSocketConfig)

  // create socket clients
  val heartbeatClientActor = actorSystem.actorOf(Props(classOf[HeartbeatClient], socketFactory),
    name = SocketType.HeartbeatClient.toString)
  val socketClientActor = actorSystem.actorOf(Props(classOf[ShellClient], socketFactory),
    name = SocketType.ShellClient.toString)

  // user should provide success/failure lambdas that act on
  // status of the ExecuteReply returned from the ask
  def execute(code: String, success: () => Unit, failure: () => Unit, result: () => Unit): Unit = {
    val request = ExecuteRequest(code, false, true, UserExpressions(), true)
    val future = actorLoader.load(MessageType.ExecuteRequest) ? request
    future.onComplete {
      case Success(_) =>
        success()
        logger.info("client resolving execute")
      case Failure(_) =>
        failure()
        logger.info("execute: something bad happened")
    }
  }

  def heartbeat(success: () => Unit, failure: () => Unit): Unit = {
    val future = heartbeatClientActor ? HeartbeatMessage
    future.onComplete {
      case Success(_) =>
        success()
        logger.info("client resolving heartbeat")
      case Failure(_) =>
        failure()
        logger.info("heartbeat: something bad happened")
    }
  }

  private def initializeRequestHandler[T](clazz: Class[T], messageType: MessageType) {
    logger.info("Creating %s handler".format(messageType.toString))
    actorSystem.actorOf(
      Props(clazz, actorLoader),
      name = messageType.toString
    )
  }

  private def initializeKernelHandlers(): Unit = {
    initializeRequestHandler(classOf[ExecuteHandler], MessageType.ExecuteRequest)
  }
  initializeKernelHandlers()
}