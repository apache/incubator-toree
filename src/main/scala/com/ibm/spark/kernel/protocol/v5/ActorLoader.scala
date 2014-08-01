package com.ibm.spark.kernel.protocol.v5

import akka.actor._
import com.ibm.spark.kernel.protocol.v5.SocketType.SocketType

/**
 * This trait defines the interface for loading actors based on some vale (enum, attribute, etc...)
 * The thought is to allow external consumers acquire actors through a common interface, minimizing the
 * spread of the logic about the Actors, ActorSystem, and other similar concepts.
 */
trait ActorLoader {
  //  TODO Unfortunately, the typdef for the enum values are the same value so we cannot overload a function definition
  //  If the signature has the original Value class as the argument there is a type error in the invocation to the
  //  method. We should try and find a better solution rather than having varying names for the method
  /**
   * This method is meant to find an actor who can properly handle a specific KernelMessage
   * based on the value of the kernels MessageType
   * @param messageType The message type for which to find an actor
   * @return An ActorSelection to pass messages to
   */
  def loadMessageActor(messageType: MessageType.MessageType) : ActorSelection
  /**
   * This method will load an actor used to communicate on one of the IPython Kernel sockets.
   * @param socketType The type of socket you want to load
   * @return An ActorSelection to pass messages to
   */
  def loadSocketActor(socketType: SocketType.SocketType) : ActorSelection
  def loadInterpreterActor() : ActorSelection
  def loadRelayActor() : ActorSelection
}

case class SimpleActorLoader(actorRefFactory : ActorRefFactory) extends ActorLoader {
  private val messageTypeActors: String = "/user/%s"

  override def loadMessageActor(messageType: MessageType.MessageType): ActorSelection = {
    actorRefFactory.actorSelection(messageTypeActors.format(messageType.toString))
  }

  override def loadSocketActor(socketType: SocketType): ActorSelection = {
    actorRefFactory.actorSelection(messageTypeActors.format(socketType.toString))
  }

  override def loadInterpreterActor(): ActorSelection = {
    actorRefFactory.actorSelection(messageTypeActors.format(SystemActorType.Interpreter.toString))
  }

  override def loadRelayActor(): ActorSelection = {
    actorRefFactory.actorSelection(messageTypeActors.format(SystemActorType.Relay.toString))
  }
}
