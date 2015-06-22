package com.ibm.spark.communication.actors

import akka.actor.{Actor, ActorRef}
import akka.util.ByteString
import com.ibm.spark.communication.{ZMQMessage, SocketManager}
import com.ibm.spark.utils.LogLike

/**
 * Represents an actor containing a subscribe socket.
 *
 * @param connection The address to connect to
 * @param listener The actor to send incoming messages back to
 */
class SubSocketActor(connection: String, listener: ActorRef)
  extends Actor with LogLike
{
  logger.debug(s"Initializing subscribe socket actor for $connection")
  private val manager: SocketManager = new SocketManager
  private val socket = manager.newSubSocket(connection, (message: Seq[String]) => {
    listener ! ZMQMessage(message.map(ByteString.apply): _*)
  })

  override def postStop(): Unit = {
    manager.closeSocket(socket)
  }

  override def receive: Actor.Receive = {
    case _ =>
  }
}
