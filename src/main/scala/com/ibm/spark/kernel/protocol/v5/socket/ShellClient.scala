package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.ExecuteReply
import play.api.libs.json.Json

import scala.collection.concurrent.{Map, TrieMap}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * The client endpoint for shell messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class ShellClient(socketFactory: SocketFactory) extends Actor with ActorLogging {
  log.debug("Created shell client actor")
  implicit val timeout = Timeout(1.minute)

  val socket  = socketFactory.ShellClient(context.system, self)
  val futureMap: Map[UUID, ActorRef] = TrieMap[UUID, ActorRef]()

  override def receive: Receive = {
    // from shell
    case message: ZMQMessage =>
      val kernelMessage: KernelMessage = message
      log.info("Shell client resolve future")
      futureMap(message.header.msg_id) ! Json.parse(kernelMessage.contentString).as[ExecuteReply]
      futureMap.remove(message.header.msg_id)

    // from handler
    case message: KernelMessage =>
      val zmq: ZMQMessage = message
      futureMap += (message.header.msg_id -> sender)
      val future = socket ? zmq
      future.onComplete {
        // future always times out because server "tells" response
        case _ => futureMap.remove(message.header.msg_id)
      }
  }
}