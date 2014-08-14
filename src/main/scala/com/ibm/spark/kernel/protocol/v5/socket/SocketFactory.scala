package com.ibm.spark.kernel.protocol.v5.socket

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.zeromq._

object SocketFactory {
  def apply(socketConfig: SocketConfig) = {
    new SocketFactory(socketConfig)
  }
}

/**
 * A Factor class to provide various socket connections for IPython Kernel Spec
 * @param socketConfig The configuration for the sockets to be properly instantiated
 */
class  SocketFactory(socketConfig: SocketConfig) {
  val HeartbeatConnection = SocketConnection(socketConfig.transport, socketConfig.ip, socketConfig.hb_port)
  val ShellConnection = SocketConnection(socketConfig.transport, socketConfig.ip, socketConfig.shell_port)
  val IOPubConnection = SocketConnection(socketConfig.transport, socketConfig.ip, socketConfig.iopub_port)

  /**
   * Creates a ZeroMQ reply socket representing the server endpoint for heartbeat messages
   * @param system The actor system the socket actor will belong
   * @param listener The actor who will receive
   * @return The ActorRef created for the socket connection
   */
  def Heartbeat(system: ActorSystem, listener: ActorRef) : ActorRef = {
    ZeroMQExtension(system).newRepSocket(Array(Listener(listener), Bind(HeartbeatConnection.toString)))
  }

  /**
   * Creates a ZeroMQ request socket representing the client endpoint for heartbeat messages
   * @param system The actor system the socket actor will belong
   * @param listener The actor who will receive
   * @return The ActorRef created for the socket connection
   */
  def HeartbeatClient(system: ActorSystem, listener: ActorRef) : ActorRef = {
    ZeroMQExtension(system).newReqSocket(Array(Listener(listener), Connect(HeartbeatConnection.toString)))
  }

  /**
   * Creates a ZeroMQ reply socket representing the server endpoint for shell messages
   * @param system The actor system the socket actor will belong
   * @param listener The actor who will receive
   * @return The ActorRef created for the socket connection
   */
  def Shell(system: ActorSystem, listener: ActorRef) : ActorRef = {
    ZeroMQExtension(system).newRouterSocket(Array(Listener(listener), Bind(ShellConnection.toString)))
  }

  /**
   * Creates a ZeroMQ request socket representing the client endpoint for shell messages.
   * Generates an id for <a href="http://api.zeromq.org/2-1:zmq-setsockopt#toc6">
   * Router/Dealer message routing</a>.
   * @param system The actor system the socket actor will belong
   * @param listener The actor who will receive
   * @return The ActorRef created for the socket connection
   */
  def ShellClient(system: ActorSystem, listener: ActorRef) : ActorRef = {
    ZeroMQExtension(system).newDealerSocket(Array(Listener(listener), Connect(ShellConnection.toString),
      Identity(UUID.randomUUID().toString.getBytes)))
  }

  /**
   * Creates a ZeroMQ reply socket representing the server endpoint for IOPub messages
   * @param system The actor system the socket actor will belong
   * @return The ActorRef created for the socket connection
   */
  def IOPub(system: ActorSystem) : ActorRef = {
    ZeroMQExtension(system).newPubSocket(Bind(IOPubConnection.toString))
  }

  /**
   * Creates a ZeroMQ request socket representing the client endpoint for IOPub messages
   * @param system The actor system the socket actor will belong
   * @param listener The actor who will receive
   * @return The ActorRef created for the socket connection
   */
  def IOPubClient(system: ActorSystem, listener: ActorRef) : ActorRef = {
    ZeroMQExtension(system).newSubSocket(Array(Listener(listener), Connect(IOPubConnection.toString), SubscribeAll))
  }
}