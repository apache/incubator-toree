package com.ibm.spark.communication.actors

import akka.actor.{Actor, ActorRef}
import akka.util.ByteString
import com.ibm.spark.communication.{ZMQMessage, SocketManager}
import com.ibm.spark.utils.LogLike

class SubSocketActor(connection: String, listener: ActorRef)
  extends Actor with LogLike
{
  logger.debug(s"Initializing subscribe socket actor for $connection")
  private val manager: SocketManager = new SocketManager
  manager.newSubSocket(connection, (message: Seq[String]) => {
    listener ! ZMQMessage(message.map(ByteString.apply): _*)
  })

  override def receive: Actor.Receive = {
    case _ =>
  }
}
