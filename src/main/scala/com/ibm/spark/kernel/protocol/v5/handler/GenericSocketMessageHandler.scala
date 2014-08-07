package com.ibm.spark.kernel.protocol.v5.handler

import akka.actor.Actor
import com.ibm.spark.kernel.protocol.v5.SocketType.SocketType
import com.ibm.spark.kernel.protocol.v5.{ActorLoader, KernelMessage}
import com.ibm.spark.utils.LogLike

/**
 * All KernelMessage leaving the kernel for the client will exit the relay in a similar pattern. This class is meant
 * to encapsulate this behaviour into one generic method. This class should be used by mapping a
 * {@link com.ibm.spark.kernel.protocol.MessageType} to the {@link com.ibm.spark.kernel.protocol.SocketType} constructor
 * parameter. This will map MessageTypes to their corresponding SocketTypes. An example us of this class would be
 *
 * actorSystem.actorOf(
 *      //  Tells the handler to forward anything it receives to the Control socket
 *      Props(classOf[GenericSocketMessageHandler], actorLoader, SocketType.Control),
 *
 *      // Creates the Actor with the name of the message type, this allows the Relay to route messages here
 *      name = MessageType.KernelInfoReply.toString
 *   )
 *
 * @param actorLoader The ActorLoader used to load the socket actors
 * @param socketType The type of socket, mapping to an Actor for this class to pass messages along to
 */
class GenericSocketMessageHandler(actorLoader: ActorLoader, socketType: SocketType)
  extends Actor with LogLike {
  override def receive: Receive = {
    case message: KernelMessage =>
      logger.debug("Sending " + message.header.msg_id + "message to " + socketType.toString + "socket")
      actorLoader.load(socketType) ! message
  }
}
