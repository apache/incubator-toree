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
package org.apache.toree.communication

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.apache.toree.communication.socket._
import org.zeromq.ZMQ

import scala.collection.JavaConverters._

/**
 * Represents the factory for sockets that also manages ZMQ contexts and
 * facilitates closing of sockets created by the factory.
 */
class SocketManager {
  /**
   * Creates a new ZMQ context with a single IO thread.
   *
   * @return The new ZMQ context
   */
  protected def newZmqContext(): ZMQ.Context = ZMQ.context(1)

  private val socketToContextMap =
    new ConcurrentHashMap[SocketLike, ZMQ.Context]().asScala

  /**
   * Provides and registers a new ZMQ context, used for creating a new socket.
   * @param mkSocket a function that creates a socket using a given context
   * @return the new socket
   * @see newZmqContext
   */
  private def withNewContext[A <: SocketLike](mkSocket: ZMQ.Context => A): A = {
    val ctx = newZmqContext()
    val socket = mkSocket(ctx)
    socketToContextMap.put(socket, ctx)
    socket
  }

  /**
   * Closes the socket provided and also closes the context if no more sockets
   * are using the context.
   *
   * @param socket The socket to close
   */
  def closeSocket(socket: SocketLike) = {
    socket.close()

    socketToContextMap.remove(socket).foreach(context => {
      if (!socketToContextMap.values.exists(_ == context)) context.close()
    })
  }

  /**
   * Creates a new request socket.
   *
   * @param address The address to associate with the socket
   * @param inboundMessageCallback The callback to use for incoming messages
   *
   * @return The new socket instance
   */
  def newReqSocket(
    address: String,
    inboundMessageCallback: (Seq[Array[Byte]]) => Unit
  ): SocketLike = withNewContext{ ctx =>
     new JeroMQSocket(new ReqSocketRunnable(
      ctx,
      Some(inboundMessageCallback),
      Connect(address),
      Linger(0)
    ))
  }

  /**
   * Creates a new reply socket.
   *
   * @param address The address to associate with the socket
   * @param inboundMessageCallback The callback to use for incoming messages
   *
   * @return The new socket instance
   */
  def newRepSocket(
    address: String,
    inboundMessageCallback: (Seq[Array[Byte]]) => Unit
  ): SocketLike = withNewContext{ ctx =>
    new JeroMQSocket(new ZeroMQSocketRunnable(
      ctx,
      RepSocket,
      Some(inboundMessageCallback),
      Bind(address),
      Linger(0)
    ))
  }

  /**
   * Creates a new publish socket.
   *
   * @param address The address to associate with the socket
   *
   * @return The new socket instance
   */
  def newPubSocket(
    address: String
  ): SocketLike = withNewContext{ ctx =>
    new JeroMQSocket(new PubSocketRunnable(
      ctx,
      Bind(address),
      Linger(0)
    ))
  }

  /**
   * Creates a new subscribe socket.
   *
   * @param address The address to associate with the socket
   * @param inboundMessageCallback The callback to use for incoming messages
   *
   * @return The new socket instance
   */
  def newSubSocket(
    address: String,
    inboundMessageCallback: (Seq[Array[Byte]]) => Unit
  ): SocketLike = withNewContext { ctx =>
    new JeroMQSocket(new ZeroMQSocketRunnable(
      ctx,
      SubSocket,
      Some(inboundMessageCallback),
      Connect(address),
      Linger(0),
      Subscribe.all
    ))
  }

  /**
   * Creates a new router socket.
   *
   * @param address The address to associate with the socket
   * @param inboundMessageCallback The callback to use for incoming messages
   *
   * @return The new socket instance
   */
  def newRouterSocket(
    address: String,
    inboundMessageCallback: (Seq[Array[Byte]]) => Unit
  ): SocketLike = withNewContext { ctx =>
    new JeroMQSocket(new ZeroMQSocketRunnable(
      ctx,
      RouterSocket,
      Some(inboundMessageCallback),
      Bind(address),
      Linger(0)
    ))
  }

  /**
   * Creates a new dealer socket.
   *
   * @param address The address to associate with the socket
   * @param inboundMessageCallback The callback to use for incoming messages
   *
   * @return The new socket instance
   */
  def newDealerSocket(
    address: String,
    inboundMessageCallback: (Seq[Array[Byte]]) => Unit,
    identity: String = UUID.randomUUID().toString
  ): SocketLike = withNewContext{ ctx =>
    new JeroMQSocket(new ZeroMQSocketRunnable(
      ctx,
      DealerSocket,
      Some(inboundMessageCallback),
      Connect(address),
      Linger(0),
      Identity(identity.getBytes(ZMQ.CHARSET))
    ))
  }
}
