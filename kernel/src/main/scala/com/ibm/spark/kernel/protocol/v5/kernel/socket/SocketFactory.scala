/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.spark.kernel.protocol.v5.kernel.socket

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
class SocketFactory(socketConfig: SocketConfig) {
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
   * Creates a ZeroMQ reply socket representing the server endpoint for shell messages
   * @param system The actor system the socket actor will belong
   * @param listener The actor who will receive
   * @return The ActorRef created for the socket connection
   */
  def Shell(system: ActorSystem, listener: ActorRef) : ActorRef = {
    ZeroMQExtension(system).newRouterSocket(Array(Listener(listener), Bind(ShellConnection.toString)))
  }

  /**
   * Creates a ZeroMQ reply socket representing the server endpoint for IOPub messages
   * @param system The actor system the socket actor will belong
   * @return The ActorRef created for the socket connection
   */
  def IOPub(system: ActorSystem) : ActorRef = {
    ZeroMQExtension(system).newPubSocket(Bind(IOPubConnection.toString))
  }
}