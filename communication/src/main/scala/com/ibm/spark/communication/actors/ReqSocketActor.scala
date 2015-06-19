package com.ibm.spark.communication.actors

import akka.actor.{Actor, ActorRef}
import akka.util.ByteString
import com.ibm.spark.communication.{ZMQMessage, SocketManager}
import com.ibm.spark.utils.LogLike
import org.zeromq.ZMQ

class ReqSocketActor(connection: String, listener: ActorRef)
  extends Actor with LogLike
{
  logger.debug(s"Initializing request socket actor for $connection")
  private val manager: SocketManager = new SocketManager
  val socket = manager.newReqSocket(connection, (message: Seq[String]) => {
    listener ! ZMQMessage(message.map(ByteString.apply): _*)
  })

  override def receive: Actor.Receive = {
    case zmqMessage: ZMQMessage =>
      val frames = zmqMessage.frames.map(byteString =>
        new String(byteString.toArray, ZMQ.CHARSET))
      socket.send(frames: _*)
  }
}
