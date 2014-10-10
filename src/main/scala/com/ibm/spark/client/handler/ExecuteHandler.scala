package com.ibm.spark.client.handler

import java.util.UUID

import akka.actor.Actor
import akka.pattern.ask
import akka.util.Timeout
import com.ibm.spark.client.ExecuteRequestTuple
import com.ibm.spark.client.handler.ExecuteHandler.resolve
import com.ibm.spark.client.message.StreamMessage
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
  implicit val timeout = Timeout(100000.days)
  private val sessionId = UUID.randomUUID().toString
  logger.info(s"Session ID for kernel client is ${sessionId}" )

  def toKernelMessage(message: ExecuteRequest): KernelMessage = {
    // construct a kernel message whose content is an ExecuteRequest
    logger.info("Creating new KernelMessage from ExecuteRequest")
    val header = Header(
      UUID.randomUUID().toString, "spark",
      sessionId, MessageType.ExecuteRequest.toString,
      "5.0"
    )
    logger.debug(s"KernelMessage created with header id ${header.msg_id}")
    KernelMessage(
      Seq[String](), "",
      header, HeaderBuilder.empty,
      Metadata(), Json.toJson(message).toString()
    )
  }

  override def receive: Receive = {
    case message: ExecuteRequest =>
      logger.info("Handling request for submit execute request.")
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
        shellReply <- shellFuture
        ioPubResult <- ioPubFuture
      } { senderRef ! resolve(ioPubResult) }

    case message: ExecuteRequestTuple =>
      logger.info("Handling request for streaming execute request.")
      // create message to send to shell
      val kernelMessage = toKernelMessage(message.request)

      // create message to send to iopub
      val streamMessage = StreamMessage(kernelMessage.header.msg_id, message.callback)

      // send the message to the ShellClient
      val shellClient = actorLoader.load(SocketType.ShellClient)
      shellClient ? kernelMessage

      // register the callback with the IOPubClient
      val ioPubClient = actorLoader.load(SocketType.IOPubClient)
      ioPubClient ! streamMessage
  }
}
