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

package org.apache.toree.kernel.protocol.v5.client.socket

import java.util.UUID

import akka.actor.{Props, ActorRef, ActorSystem}
import org.apache.toree.communication.actors.{DealerSocketActor, ReqSocketActor, SubSocketActor}

object SocketFactory {
  def apply(socketConfig: SocketConfig) = {
    new SocketFactory(socketConfig)
  }
}

/**
 * A Factor class to provide various socket connections for IPython Kernel Spec.
 *
 * @param socketConfig The configuration for the sockets to be properly
 *                     instantiated
 */
class  SocketFactory(socketConfig: SocketConfig) {
  /**
   * Represents the identity shared between Shell and Stdin connections.
   */
  private val ZmqIdentity = UUID.randomUUID().toString

  val HeartbeatConnection = SocketConnection(
    socketConfig.transport, socketConfig.ip, socketConfig.hb_port)
  val ShellConnection = SocketConnection(
    socketConfig.transport, socketConfig.ip, socketConfig.shell_port)
  val IOPubConnection = SocketConnection(
    socketConfig.transport, socketConfig.ip, socketConfig.iopub_port)
  val StdinConnection = SocketConnection(
    socketConfig.transport, socketConfig.ip, socketConfig.stdin_port)

  /**
   * Creates a ZeroMQ request socket representing the client endpoint for
   * heartbeat messages.
   *
   * @param system The actor system the socket actor will belong
   * @param listener The actor who will receive
   *
   * @return The ActorRef created for the socket connection
   */
  def HeartbeatClient(system: ActorSystem, listener: ActorRef) : ActorRef = {
    system.actorOf(Props(classOf[ReqSocketActor], HeartbeatConnection.toString, listener))
//    ZeroMQExtension(system).newReqSocket(Array(
//      Listener(listener), Connect(HeartbeatConnection.toString)
//    ))
  }

  /**
   * Creates a ZeroMQ request socket representing the client endpoint for shell
   * messages. Generates an id for
   * <a href="http://api.zeromq.org/2-1:zmq-setsockopt#toc6">
   * Router/Dealer message routing</a>.
   *
   * @param system The actor system the socket actor will belong
   * @param listener The actor who will receive
   *
   * @return The ActorRef created for the socket connection
   */
  def ShellClient(system: ActorSystem, listener: ActorRef) : ActorRef = {
    system.actorOf(Props(classOf[DealerSocketActor], ShellConnection.toString, listener))
    //socket.setIdentity(ZmqIdentity)
//    ZeroMQExtension(system).newDealerSocket(Array(
//      Listener(listener), Connect(ShellConnection.toString),
//      Identity(ZmqIdentity)
//    ))
  }

  /**
   * Creates a ZeroMQ reply socket representing the client endpoint for stdin
   * messages. Generates an id for
   * <a href="http://api.zeromq.org/2-1:zmq-setsockopt#toc6">
   * Router/Dealer message routing</a>.
   *
   * @param system The actor system the socket actor will belong
   * @param listener The actor who will receive
   *
   * @return The ActorRef created for the socket connection
   */
  def StdinClient(system: ActorSystem, listener: ActorRef) : ActorRef = {
    system.actorOf(Props(classOf[DealerSocketActor], StdinConnection.toString, listener))
    //socket.setIdentity(ZmqIdentity)
//    ZeroMQExtension(system).newDealerSocket(Array(
//      Listener(listener), Connect(StdinConnection.toString),
//      Identity(ZmqIdentity)
//    ))
  }

  /**
   * Creates a ZeroMQ request socket representing the client endpoint for IOPub
   * messages.
   *
   * @param system The actor system the socket actor will belong
   * @param listener The actor who will receive
   *
   * @return The ActorRef created for the socket connection
   */
  def IOPubClient(system: ActorSystem, listener: ActorRef) : ActorRef = {
    system.actorOf(Props(classOf[SubSocketActor], IOPubConnection.toString, listener))
    //socket.subscribe(ZMQ.SUBSCRIPTION_ALL)
//    ZeroMQExtension(system).newSubSocket(Array(
//      Listener(listener), Connect(IOPubConnection.toString), SubscribeAll
//    ))
  }
}