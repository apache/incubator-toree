package com.ibm.spark.kernel.protocol.v5.client.handler

import java.util.UUID
import akka.actor.Actor
import akka.pattern.ask
import akka.util.Timeout
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.client.ExecuteRequestTuple
import com.ibm.spark.kernel.protocol.v5.client.message.StreamMessage
import com.ibm.spark.kernel.protocol.v5.content._
import com.ibm.spark.utils.LogLike
import play.api.libs.json.Json
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object ExecuteHandler extends LogLike {
  private[client] def resolve(shellReply: ExecuteReply, ioPubResult: Any): Option[Either[ExecuteReplyError, ExecuteResult]] = {
    println("shellReply: " + shellReply)
    shellReply.status match {
      //case errorReply: ExecuteReplyError =>
      case "error" =>
        Some(Left(shellReply))
      case _ =>
        ioPubResult match {
          // all of these shell messages will be handled in client with callbacks
          // forward result to client
          case result: ExecuteResult =>
            Some(Right(result))

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
}

/**
 * Actor for handling client execute request and reply messages
 */
class ExecuteHandler(actorLoader: ActorLoader) extends Actor with LogLike {
  implicit val timeout = Timeout(21474835.seconds)
  private val sessionId = UUID.randomUUID().toString

  def toKernelMessage(message: ExecuteRequest): KernelMessage = {
    // construct a kernel message whose content is an ExecuteRequest
    val id = UUID.randomUUID().toString
    val header = Header(
      id, "spark",
      sessionId, MessageType.ExecuteRequest.toString,
      "5.0"
    )
    KernelMessage(
      Seq[String](), "",
      header, HeaderBuilder.empty,
      Metadata(), Json.toJson(message).toString()
    )
  }

  override def receive: Receive = {
    case message: ExecuteRequest =>
      // create message to send to shell
      val kernelMessage = toKernelMessage(message)

      // use to access sender in future
      val senderRef = sender

      // send the message to the ShellClient
      val shellClient = actorLoader.load(SocketType.ShellClient)
      val shellFuture = shellClient ? kernelMessage

      // register ourselves with iopub to resolve the ask
      val ioPubClient = actorLoader.load(SocketType.IOPubClient)
      val ioPubFuture = ioPubClient ? kernelMessage.header.msg_id

      for {
        shellReply  <- shellFuture.mapTo[ExecuteReply]
        ioPubResult <- ioPubFuture
      } { ExecuteHandler.resolve(shellReply, ioPubResult) match {
        case None =>
        //do nothing
        case Some(either) =>
          senderRef ! either
      }  }

    case message: ExecuteRequestTuple =>
      // create message to send to shell
      val kernelMessage = toKernelMessage(message.request)

      // create message to send to iopub
      val streamMessage = StreamMessage(kernelMessage.header.msg_id, message.callback)

      // use to access sender in future
      val senderRef = sender // new

      // send the message to the ShellClient
      val shellClient = actorLoader.load(SocketType.ShellClient)
      val shellFuture = shellClient ? kernelMessage

      // registers the following with the IOPubClient:
      //  - the callback for Stream messages
      //  - the message id for ExecuteResult messages
      val ioPubClient = actorLoader.load(SocketType.IOPubClient)
      val ioPubFuture = ioPubClient ? streamMessage

      for {
        shellReply  <- shellFuture.mapTo[ExecuteReply]
        ioPubResult <- ioPubFuture
      } { ExecuteHandler.resolve(shellReply, ioPubResult) match {
        case None =>
        //do nothing
        case Some(either) =>
          senderRef ! either
      } }
  }
}