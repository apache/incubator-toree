package com.ibm.spark.kernel.protocol.v5

import akka.actor._
import com.ibm.spark.kernel.protocol.v5.MessageType._

/**
 * This trait defines the interface for loading actors based on some vale (enum, attribute, etc...)
 * The thought is to allow external consumers acquire actors through a common interface, minimizing the
 * spread of the logic about the Actors, ActorSystem, and other similar concepts.
 */
trait ActorLoader {
  /**
   * This method is meant to find an actor who can properly handle a specific KernelMessage
   * based on the value of the kernels MessageType
   * @param messageType The message type for which to find an actor
   * @return An ActorRef to pass the message along
   */
def load(messageType: MessageType) : ActorSelection
}

case class SimpleActorLoader(actorRefFactory : ActorRefFactory) extends ActorLoader {
  private val messageTypeActors: String = "/user/%s"

  override def load(messageType: MessageType): ActorSelection = {
    actorRefFactory.actorSelection(messageTypeActors.format(messageType.toString))
  }
}
