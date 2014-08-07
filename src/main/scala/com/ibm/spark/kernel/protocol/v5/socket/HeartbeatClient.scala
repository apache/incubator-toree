package com.ibm.spark.kernel.protocol.v5.socket

import java.util.UUID

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.{ByteString, Timeout}
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5
import com.ibm.spark.utils.LogLike

import scala.collection.concurrent.{Map, TrieMap}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object HeartbeatMessage {}
/**
 * The client endpoint for heartbeat messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class HeartbeatClient(socketFactory : SocketFactory) extends Actor with LogLike {
  logger.debug("Created new Heartbeat Client actor")
  implicit val timeout = Timeout(1.minute)

  val futureMap: Map[v5.UUID, ActorRef] = TrieMap[v5.UUID, ActorRef]()
  val socket = socketFactory.HeartbeatClient(context.system, self)

  override def receive: Receive = {
    // from Heartbeat
    case message: ZMQMessage =>
      val id = message.frames.map((byteString: ByteString) => new String(byteString.toArray)).mkString("\n")
      logger.info(s"Heartbeat client receive:$id")
      futureMap(id) ! true
      futureMap.remove(id)

    // from SparkKernelClient
    case HeartbeatMessage =>
      val id = UUID.randomUUID().toString
      futureMap += (id -> sender)
      logger.info(s"Heartbeat client send: $id")
      val future = socket ? ZMQMessage(ByteString(id.getBytes))
      future.onComplete {
        // future always times out because server "tells" response {
        case(_) => futureMap.remove(id)
      }
  }
}