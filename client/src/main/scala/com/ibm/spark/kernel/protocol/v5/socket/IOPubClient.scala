package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.{Actor, ActorRef}
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5.MessageType.MessageType
import com.ibm.spark.kernel.protocol.v5.Utilities._
import com.ibm.spark.kernel.protocol.v5.client.message.StreamMessage
import com.ibm.spark.kernel.protocol.v5.content.{ExecuteResult, StreamContent}
import com.ibm.spark.kernel.protocol.v5.socket.IOPubClient._
import com.ibm.spark.kernel.protocol.v5.{KernelMessage, MessageType, UUID}
import com.ibm.spark.utils.LogLike
import play.api.libs.json.Json

import scala.collection.concurrent.{Map, TrieMap}

object IOPubClient {
  private val senderMap: Map[UUID, ActorRef] = TrieMap[UUID, ActorRef]()
  private val callbackMap: Map[UUID, Any => Unit] = TrieMap[UUID, Any => Unit]()
}

/**
 * The client endpoint for IOPub messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class IOPubClient(socketFactory: ClientSocketFactory) extends Actor with LogLike {
  private val socket = socketFactory.IOPubClient(context.system, self)
  logger.info("Created IOPub socket")

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
          // look up callback in CallbackMap based on msg_id and invoke
          val id = kernelMessage.parentHeader.msg_id
          val client = senderMap.get(id)
          client match {
            case Some(actorRef) =>
              actorRef ! Json.parse(kernelMessage.contentString).as[ExecuteResult]
              senderMap.remove(id)
            case None =>
              logger.debug("IOPubClient: actorRef was none")
          }

        case MessageType.Stream =>
          val id = kernelMessage.parentHeader.msg_id
          val func = callbackMap.get(id)
          func match {
            case Some(f) =>
              val streamContent = Json.parse(kernelMessage.contentString).as[StreamContent]
              f(streamContent.text)
            case None =>
              logger.debug(s"IOPubClient: no function for id ${id}")
          }

        case any =>
          logger.debug(s"Received unhandled MessageType ${any}")
      }

    case message: UUID =>
      senderMap.put(message, sender)

    case message: StreamMessage => {
      logger.trace("Registering Stream message information.")
      logger.trace(s"Message id is ${message.id}")
      // registers with the senderMap for ExecuteResult
      senderMap.put(message.id, sender)
      // registers with the callbackMap for Stream
      callbackMap.put(message.id, message.callback)

      logger.debug("Registered Stream message information.")
    }
  }
}
