package com.ibm.spark.kernel.protocol.v5

import akka.actor.Actor
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5.MessageType.MessageType

/**
 * This class is meant to be a relay for send KernelMessages through kernel system.
 * @param actorLoader The ActorLoader used by this class for finding actors for relaying messages
 */
case class Relay(actorLoader: ActorLoader) extends Actor {
  /**
   * Relays a KernelMessage to a specific actor to handle that message
   * @param kernelMessage The message to relay
   */
  private def relay(kernelMessage: KernelMessage){
    val messageType: MessageType = MessageType.withName(kernelMessage.header.msg_type)
    actorLoader.loadMessageActor(messageType) ! kernelMessage
  }

  /**
   * This actor will receive and handle two types; ZMQMessage and KernelMessage. These messages
   * will be forwarded to their actors which are responsible for them.
   */
  override def receive = {
    //  We need to have these cases explicitly because the implicit to convert is only
    //  done when we call the method. Hence, the two cases
    case zmqMessage: ZMQMessage =>
      relay(zmqMessage)
    case kernelMessage: KernelMessage =>
      relay(kernelMessage)
    case unknown =>
      //  TODO handle the unknown case
  }
}
