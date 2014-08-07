package com.ibm.spark.client

import java.util.UUID

import akka.actor.{Actor, ActorLogging}
import akka.pattern.ask
import akka.util.Timeout
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.{ExecuteReply, ExecuteRequest}
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * Actor for handling client execute request and reply messages
 */
class ExecuteHandler(actorLoader: ActorLoader) extends Actor with ActorLogging {
  implicit val timeout = Timeout(1.minute)

  override def receive: Receive = {
    case message: ExecuteRequest =>
      val header = Header(UUID.randomUUID().toString, "spark", UUID.randomUUID().toString, MessageType.ExecuteRequest.toString, "5.0")
      val kernelMessage = KernelMessage(Seq[String](), "", header, EmptyHeader, Metadata(), Json.toJson(message).toString)
      val futureSender = sender // used to access sender in future
      val future = actorLoader.load(SocketType.ShellClient) ? kernelMessage

      future.onComplete {
        case Success(message: ExecuteReply) =>
          futureSender ! message
        case Failure(_) =>
          log.error("execute handler fail")
          // todo error handling
        case Success(_) =>
          log.error("Future success with wrong message type")
      }
  }
}