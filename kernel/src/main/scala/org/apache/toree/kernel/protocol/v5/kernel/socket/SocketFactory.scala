/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package org.apache.toree.kernel.protocol.v5.kernel.socket

import akka.actor.{Props, ActorRef, ActorSystem}
import org.apache.toree.communication.actors.{RouterSocketActor, RepSocketActor, PubSocketActor}

object SocketFactory {
  def apply(socketConfig: SocketConfig) = {
    new SocketFactory(socketConfig)
  }
}

/**
 * A Factory class to provide various socket connections for IPython Kernel Spec
 * @param socketConfig The configuration for the sockets to be properly
 *                     instantiated
 */
class SocketFactory(socketConfig: SocketConfig) {
  val HeartbeatConnection = SocketConnection(
    socketConfig.transport, socketConfig.ip, socketConfig.hb_port)
  val ShellConnection = SocketConnection(
    socketConfig.transport, socketConfig.ip, socketConfig.shell_port)
  val ControlConnection = SocketConnection(
    socketConfig.transport, socketConfig.ip, socketConfig.control_port)
  val IOPubConnection = SocketConnection(
    socketConfig.transport, socketConfig.ip, socketConfig.iopub_port)
  val StdinConnection = SocketConnection(
    socketConfig.transport, socketConfig.ip, socketConfig.stdin_port)

  /**
   * Creates a ZeroMQ reply socket representing the server endpoint for
   * heartbeat messages
   * @param system The actor system the socket actor will belong
   * @param listener The actor who will receive
   * @return The ActorRef created for the socket connection
   */
  def Heartbeat(system: ActorSystem, listener: ActorRef) : ActorRef =
    system.actorOf(Props(classOf[RepSocketActor], HeartbeatConnection.toString, listener))
//    ZeroMQExtension(system).newRepSocket(
//      Array(Listener(listener), Bind(HeartbeatConnection.toString))
//    )

  /**
   * Creates a ZeroMQ reply socket representing the server endpoint for shell
   * messages
   * @param system The actor system the socket actor will belong
   * @param listener The actor who will receive
   * @return The ActorRef created for the socket connection
   */
  def Shell(system: ActorSystem, listener: ActorRef) : ActorRef =
    system.actorOf(Props(classOf[RouterSocketActor], ShellConnection.toString, listener))
//    ZeroMQExtension(system).newRouterSocket(
//      Array(Listener(listener), Bind(ShellConnection.toString))
//    )

  /**
   * Creates a ZeroMQ reply socket representing the server endpoint for control
   * messages
   * @param system The actor system the socket actor will belong
   * @param listener The actor who will receive
   * @return The ActorRef created for the socket connection
   */
  def Control(system: ActorSystem, listener: ActorRef) : ActorRef =
    system.actorOf(Props(classOf[RouterSocketActor], ControlConnection.toString, listener))

  /**
   * Creates a ZeroMQ reply socket representing the server endpoint for stdin
   * messages
   * @param system The actor system the socket actor will belong
   * @param listener The actor who will receive
   * @return The ActorRef created for the socket connection
   */
  def Stdin(system: ActorSystem, listener: ActorRef) : ActorRef =
    system.actorOf(Props(classOf[RouterSocketActor], StdinConnection.toString, listener))
//    ZeroMQExtension(system).newRouterSocket(
//      Array(Listener(listener), Bind(StdinConnection.toString))
//    )

  /**
   * Creates a ZeroMQ reply socket representing the server endpoint for IOPub
   * messages
   * @param system The actor system the socket actor will belong
   * @return The ActorRef created for the socket connection
   */
  def IOPub(system: ActorSystem) : ActorRef =
    system.actorOf(Props(classOf[PubSocketActor], IOPubConnection.toString))
//    ZeroMQExtension(system).newPubSocket(
//      Bind(IOPubConnection.toString)
//    )
}