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
package org.apache.toree.communication.actors

import akka.actor.{Actor, ActorRef}
import akka.util.ByteString
import org.apache.toree.communication.{SocketManager, ZMQMessage}
import org.apache.toree.utils.LogLike
import org.zeromq.ZMQ

/**
 * Represents an actor containing a router socket.
 *
 * @param connection The address to bind to
 * @param listener The actor to send incoming messages back to
 */
class RouterSocketActor(connection: String, listener: ActorRef)
  extends Actor with LogLike
{
  logger.debug(s"Initializing router socket actor for $connection")
  private val manager: SocketManager = new SocketManager
  private val socket = manager.newRouterSocket(connection, (message: Seq[Array[Byte]]) => {
    listener ! ZMQMessage(message.map(ByteString.apply): _*)
  })

  override def postStop(): Unit = {
    manager.closeSocket(socket)
  }

  override def receive: Actor.Receive = {
    case zmqMessage: ZMQMessage =>
      val frames = zmqMessage.frames.map(byteString => byteString.toArray )
    socket.send(frames: _*)
  }
}
