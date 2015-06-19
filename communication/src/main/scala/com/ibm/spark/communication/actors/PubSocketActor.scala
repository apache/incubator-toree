package com.ibm.spark.communication.actors

import akka.actor.Actor
import com.ibm.spark.communication.{SocketManager, ZMQMessage}
import com.ibm.spark.utils.LogLike
import org.zeromq.ZMQ

class PubSocketActor(connection: String)
  extends Actor with LogLike
{
  logger.debug(s"Initializing publish socket actor for $connection")
  private val manager: SocketManager = new SocketManager
  val socket = manager.newPubSocket(connection)

  override def receive: Actor.Receive = {
    case zmqMessage: ZMQMessage =>
      val frames = zmqMessage.frames.map(byteString =>
        new String(byteString.toArray, ZMQ.CHARSET))
      socket.send(frames: _*)
  }
}
