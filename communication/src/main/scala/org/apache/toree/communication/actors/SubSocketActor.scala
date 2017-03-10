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
import org.apache.toree.communication.{ZMQMessage, SocketManager}
import org.apache.toree.utils.LogLike

/**
 * Represents an actor containing a subscribe socket.
 *
 * @param connection The address to connect to
 * @param listener The actor to send incoming messages back to
 */
class SubSocketActor(connection: String, listener: ActorRef)
  extends Actor with LogLike
{
  logger.debug(s"Initializing subscribe socket actor for $connection")
  private val manager: SocketManager = new SocketManager
  private val socket = manager.newSubSocket(connection, (message: Seq[Array[Byte]]) => {
    listener ! ZMQMessage(message.map(ByteString.apply): _*)
  })

  override def postStop(): Unit = {
    manager.closeSocket(socket)
  }

  override def receive: Actor.Receive = {
    case _ =>
  }
}
