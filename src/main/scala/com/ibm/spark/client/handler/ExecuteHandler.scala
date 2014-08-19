package com.ibm.spark.client.handler

import java.util.UUID

import akka.actor.Actor
import akka.pattern.ask
import akka.util.Timeout
import com.ibm.spark.client.handler.ExecuteHandler.resolve
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.{ExecuteRequest, ExecuteResult}
import com.ibm.spark.utils.LogLike
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object ExecuteHandler extends LogLike {
  private[client] def resolve(result: Any): Option[ExecuteResult] = {
    result match {
      // all of these shell messages will be handled in client with callbacks
      // forward result to client
      case result: ExecuteResult =>
        Option(result)

      // rethrow exception so client can handle it
      case result: Throwable =>
        logger.debug(s"Rethrowing ${result} to client")
        throw result

      // swallow all unsupported (untyped) successes
      case result =>
        logger.debug(s"Swallowing unhandled (${result.getClass.toString})")
        None
    }
  }
}

/**
 * Actor for handling client execute request and reply messages
 */
class ExecuteHandler(actorLoader: ActorLoader) extends Actor with LogLike {
  implicit val timeout = Timeout(1.minutes)
  private val sessionId = UUID.randomUUID().toString

  override def receive: Receive = {
    // ExecuteRequest, ExecuteReply callback, ExecuteResult callback
    case message: ExecuteRequest =>
      // construct the message to send
      val id = UUID.randomUUID().toString
      val header = Header(id, "spark", sessionId, MessageType.ExecuteRequest.toString, "5.0")
      val kernelMessage = KernelMessage(Seq[String](), "", header, DefaultHeader, Metadata(), Json.toJson(message).toString)

      // use to access sender in future
      val senderRef = sender

      // send the message to the ShellClient
      val shellFuture = actorLoader.load(SocketType.ShellClient) ? kernelMessage
      val ioPubFuture = actorLoader.load(SocketType.IOPubClient) ? id

      for {
        shellReply <- shellFuture
        ioPubResult <- ioPubFuture
      } { senderRef ! resolve(ioPubResult) }
  }
}