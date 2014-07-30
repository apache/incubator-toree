package com.ibm.kernel.protocol.v5.socket

import akka.actor.{ScalaActorRef, ActorRef, ActorSystem}
import akka.zeromq.{Connect, Bind, ZeroMQExtension, Listener}

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

  /**
   * Creates a ZeroMQ reply socket representing the server endpoint for heartbeat messages
   * @param system The actor system the socket actor will belong
   * @param listener The actor who will receive
   * @return The ActorRef created for the socket connection
   */
  def Heartbeat(system: ActorSystem, listener: ScalaActorRef) : ActorRef = {
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
}
