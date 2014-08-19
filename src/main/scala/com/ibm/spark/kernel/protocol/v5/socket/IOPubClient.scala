package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.{Actor, ActorRef}
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5.MessageType._
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.ExecuteResult
import com.ibm.spark.utils.LogLike
import play.api.libs.json.Json
import com.ibm.spark.kernel.protocol.v5.socket.IOPubClient._
import scala.collection.concurrent.{Map, TrieMap}

object IOPubClient {
  private val futureMap: Map[UUID, ActorRef] = TrieMap[UUID, ActorRef]()
}

/**
 * The client endpoint for IOPub messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class IOPubClient(socketFactory: SocketFactory) extends Actor with LogLike {
  private val socket = socketFactory.IOPubClient(context.system, self)

  override def receive: Receive = {
    case message: ZMQMessage =>
      // convert to KernelMessage using implicits in v5
      val kernelMessage: KernelMessage = message
      val messageType: MessageType = MessageType.withName(kernelMessage.header.msg_type)

      messageType match {
        case MessageType.ExecuteResult =>
          // look up callback in CallbackMap based on msg_id and invoke
          val id = kernelMessage.header.msg_id
          val client = futureMap.get(id)
          client match {
            case Some(actorRef) =>
              actorRef ! Json.parse(kernelMessage.contentString).as[ExecuteResult]
              futureMap.remove(id)
            case None =>
              logger.debug("IOPubClient: actorRef was none")
          }
      }

    case message: UUID =>
      futureMap.put(message, sender)
  }
}
