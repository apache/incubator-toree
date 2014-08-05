package com.ibm.spark.kernel.protocol.v5

import akka.actor._

/**
 * This trait defines the interface for loading actors based on some vale (enum, attribute, etc...)
 * The thought is to allow external consumers acquire actors through a common interface, minimizing the
 * spread of the logic about the Actors, ActorSystem, and other similar concepts.
 */
trait ActorLoader {
  /**
   * This method is meant to find an actor associated with an enum value. This enum value can map to an actor
   * associated with handling a specific kernel message, a socket type, or other functionality.
   * @param actorEnum The enum value used to load the actor
   * @return An ActorSelection to pass messages to
   */
  def load(actorEnum: Enumeration#Value) : ActorSelection
}

case class SimpleActorLoader(actorRefFactory : ActorRefFactory) extends ActorLoader {
  private val userActorDirectory: String = "/user/%s"

  override def load(actorEnum: Enumeration#Value): ActorSelection = {
    actorRefFactory.actorSelection(
      userActorDirectory.format(actorEnum.toString)
    )
  }

}
