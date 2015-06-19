package com.ibm.spark.communication.actors

import akka.actor.{Actor, ActorRef}
import akka.util.ByteString
import com.ibm.spark.communication.{SocketManager, ZMQMessage}
import com.ibm.spark.utils.LogLike
import org.zeromq.ZMQ

class RepSocketActor(connection: String, listener: ActorRef)
  extends Actor with LogLike
{
  logger.debug(s"Initializing reply socket actor for $connection")
  private val manager: SocketManager = new SocketManager
  val socket = manager.newRepSocket(connection, (message: Seq[String]) => {
    listener ! ZMQMessage(message.map(ByteString.apply): _*)
  })

  override def receive: Actor.Receive = {
    case zmqMessage: ZMQMessage =>
      val frames = zmqMessage.frames.map(byteString =>
        new String(byteString.toArray, ZMQ.CHARSET))
      socket.send(frames: _*)
  }
}
