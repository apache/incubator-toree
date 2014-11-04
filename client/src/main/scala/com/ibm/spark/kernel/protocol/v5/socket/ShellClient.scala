package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5.Utilities._
import com.ibm.spark.kernel.protocol.v5.content.ExecuteReply
import com.ibm.spark.kernel.protocol.v5.socket.ShellClient._
import com.ibm.spark.kernel.protocol.v5.{KernelMessage, UUID}
import com.ibm.spark.utils.LogLike
import play.api.libs.json.Json
import scala.concurrent.duration._
import scala.collection.concurrent.{Map, TrieMap}
import scala.concurrent.ExecutionContext.Implicits.global

object ShellClient {
  private val futureMap: Map[UUID, ActorRef] = TrieMap[UUID, ActorRef]()
}

/**
 * The client endpoint for Shell messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class ShellClient(socketFactory: ClientSocketFactory) extends Actor with LogLike {
  logger.debug("Created shell client actor")
  implicit val timeout = Timeout(21474835.seconds)

  val socket = socketFactory.ShellClient(context.system, self)

  override def receive: Receive = {
    // from shell
    case message: ZMQMessage =>
      logger.debug("Received shell kernel message.")
      val kernelMessage: KernelMessage = message
      logger.trace(s"Kernel message is ${kernelMessage}")
      futureMap(kernelMessage.parentHeader.msg_id) ! Json.parse(kernelMessage.contentString).as[ExecuteReply]
      futureMap.remove(kernelMessage.parentHeader.msg_id)

    // from handler
    case message: KernelMessage =>
      logger.debug("Sending shell kernel message.")
      logger.trace(s"Kernel message is ${message}")
      val zmq: ZMQMessage = message
      futureMap += (message.header.msg_id -> sender)
      val future = socket ? zmq
      future.onComplete {
        // future always times out since server "tells" response; remove key
        case _ => futureMap.remove(message.header.msg_id)
      }
  }
}
