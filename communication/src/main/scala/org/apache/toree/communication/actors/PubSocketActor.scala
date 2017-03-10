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

import akka.actor.Actor
import org.apache.toree.communication.utils.OrderedSupport
import org.apache.toree.communication.{SocketManager, ZMQMessage}
import org.apache.toree.kernel.protocol.v5.KernelMessage
import org.apache.toree.utils.LogLike
import org.zeromq.ZMQ

/**
 * Represents an actor containing a publish socket.
 *
 * Note: OrderedSupport is used to ensure correct processing order.
 *       A similar pattern may be useful for other socket actors if
 *       issues arise in the future.
 *
 * @param connection The address to bind to
 */
class PubSocketActor(connection: String)
  extends Actor with LogLike with OrderedSupport
{
  logger.debug(s"Initializing publish socket actor for $connection")
  private val manager: SocketManager = new SocketManager
  private val socket = manager.newPubSocket(connection)

  override def postStop(): Unit = {
    manager.closeSocket(socket)
  }

  override def receive: Actor.Receive = {
    case zmqMessage: ZMQMessage => withProcessing {
      val frames = zmqMessage.frames.map(byteString => byteString.toArray )

      socket.send(frames: _*)
    }
  }

  /**
   * Defines the types that will be stashed by [[waiting]]
   * while the Actor is in processing state.
   * @return
   */
  override def orderedTypes(): Seq[Class[_]] = Seq(classOf[ZMQMessage])
}
