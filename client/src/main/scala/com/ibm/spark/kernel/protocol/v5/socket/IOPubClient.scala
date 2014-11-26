package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.Status.Success
import akka.actor.{Actor, ActorRef}
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5.MessageType.MessageType
import com.ibm.spark.kernel.protocol.v5.Utilities._
import com.ibm.spark.kernel.protocol.v5.client.message.StreamMessage
import com.ibm.spark.kernel.protocol.v5.content.{KernelStatus, ExecuteResult, StreamContent}
import com.ibm.spark.kernel.protocol.v5.socket.IOPubClient._
import com.ibm.spark.kernel.protocol.v5.{KernelMessage, MessageType, UUID}
import com.ibm.spark.utils.LogLike
import play.api.data.validation.ValidationError
import play.api.libs.json.{Reads, JsPath, Json}

import scala.collection.concurrent.{Map, TrieMap}
import scala.util.Failure

object IOPubClient {
  private val senderMap: Map[UUID, ActorRef] = TrieMap[UUID, ActorRef]()
  private val callbackMap: Map[UUID, Any => Unit] = TrieMap[UUID, Any => Unit]()
  val PARENT_HEADER_NULL_MESSAGE = "Parent Header was null in Kernel Message."
}

/**
 * The client endpoint for IOPub messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class IOPubClient(socketFactory: ClientSocketFactory) extends Actor with LogLike {
  private val socket = socketFactory.IOPubClient(context.system, self)
  logger.info("Created IOPub socket")

  def receiveKernelMessage(kernelMessage: KernelMessage, func: (String)=>Unit): Unit = {
    if(kernelMessage.parentHeader != null){
      func(kernelMessage.parentHeader.msg_id)
      sender().forward(Success)
    } else {
      sender().forward(Failure(new RuntimeException(PARENT_HEADER_NULL_MESSAGE)))
    }

  }
  def parseAndHandle[T](json: String, reads: Reads[T], handler: T => Unit) : Unit = {
    Json.parse(json).validate[T](reads).fold(
      (invalid: Seq[(JsPath, Seq[ValidationError])]) =>
        logger.error(s"Could not parse JSON, ${json}"),
      (content: T) => handler(content)
    )
  }

  def receiveStreamMessage(parentHeaderId:String, kernelMessage: KernelMessage): Unit ={
    // look up callback in CallbackMap based on msg_id and invoke
    val func = callbackMap.get(parentHeaderId)
    func match {
      case Some(streamCallback) => parseAndHandle(kernelMessage.contentString, StreamContent.inspectRequestReads,
        (streamContent: StreamContent) => streamCallback(streamContent))
      case None =>
        logger.warn(s"No stream callback function for message with parent header id ${parentHeaderId}")
    }
  }

  def receiveExecuteResult(parentHeaderId:String, kernelMessage: KernelMessage): Unit = {
    val client = senderMap.get(parentHeaderId)
    client match {
      case Some(actorRef) => parseAndHandle(kernelMessage.contentString, ExecuteResult.executeResultReads,
          (res: ExecuteResult) => { actorRef ! res; senderMap.remove(parentHeaderId);})
      case None =>
        logger.warn(s"Sender actor ref was none for message with parent header id ${parentHeaderId}")
    }
  }

  override def receive: Receive = {
    case message: ZMQMessage =>
      // convert to KernelMessage using implicits in v5
      logger.debug("Received IOPub kernel message.")
      val kernelMessage: KernelMessage = message
      logger.trace(s"Kernel message is ${kernelMessage}")
      val messageType: MessageType = MessageType.withName(kernelMessage.header.msg_type)
      logger.trace(s"Message type is ${messageType}")

      messageType match {
        case MessageType.ExecuteResult =>
          receiveKernelMessage(kernelMessage, receiveExecuteResult(_, kernelMessage))
        case MessageType.Stream =>
          receiveKernelMessage(kernelMessage, receiveStreamMessage(_, kernelMessage))
        case any =>
          logger.warn(s"Received unhandled MessageType ${any}")
      }

    case message: UUID =>
      senderMap.put(message, sender)

    case message: StreamMessage => {
      logger.trace(s"Registering sender and callback information for StreamMessage with id ${message.id}")
      // registers with the senderMap for ExecuteResult
      senderMap.put(message.id, sender)
      // registers with the callbackMap for Stream
      callbackMap.put(message.id, message.callback)

      logger.trace("Registered Stream message information.")
    }
  }
}
