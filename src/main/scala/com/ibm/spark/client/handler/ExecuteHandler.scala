package com.ibm.spark.client.handler

import java.util.UUID

import akka.actor.Actor
import akka.pattern.ask
import akka.util.Timeout
import com.ibm.spark.client.handler.ExecuteHandler.resolve
import com.ibm.spark.client.{CallbackMap, ExecuteRequestTuple}
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.ExecuteReply
import com.ibm.spark.utils.LogLike
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object ExecuteHandler extends LogLike {
  private[client] def resolve(result: Try[Any]): Option[ExecuteReply] = {
    result match {
      // all of these shell messages will be handled in client with callbacks
      // forward result to client
      case Success(executeReply: ExecuteReply) =>
        Option(executeReply)

      // rethrow exception so client can handle it
      case Failure(throwable) =>
        logger.debug(s"Rethrowing ${throwable} to client")
        throw throwable

      // swallow all unsupported (untyped) successes
      case Success(message) =>
        logger.debug(s"Swallowing unhandled (${message.getClass.toString})")
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
    case message: ExecuteRequestTuple =>
      // construct the message to send
      val header = Header(UUID.randomUUID().toString, "spark", sessionId, MessageType.ExecuteRequest.toString, "5.0")
      val kernelMessage = KernelMessage(Seq[String](), "", header, DefaultHeader, Metadata(), Json.toJson(message.request).toString)

      // put callbacks to invoke on response from ShellClient and IOPubClient
      CallbackMap.put(header.msg_id, message.resultCallback)

      // use to access sender in future
      val senderRef = sender

      // send the message to the ShellClient
      val future = actorLoader.load(SocketType.ShellClient) ? kernelMessage

      future.onComplete {
        case result =>
          val executeReply = resolve(result)
          if(executeReply != None)
            senderRef ! executeReply.get
      }
  }
}