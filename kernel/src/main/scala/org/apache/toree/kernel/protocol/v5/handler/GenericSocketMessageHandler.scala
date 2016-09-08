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

package org.apache.toree.kernel.protocol.v5.handler

import akka.actor.Actor
import org.apache.toree.communication.utils.OrderedSupport
import org.apache.toree.kernel.protocol.v5.KernelMessage
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.utils.{MessageLogSupport, LogLike}

/**
 * All KernelMessage leaving the kernel for the client will exit the relay in a similar pattern. This class is meant
 * to encapsulate this behaviour into one generic method. This class should be used by mapping a
 * `org.apache.toree.kernel.protocol.v5.MessageType` to the `org.apache.toree.kernel.protocol.v5.SocketType` constructor
 * parameter. This will map MessageTypes to their corresponding SocketTypes. An example us of this class would be
 *
 * actorSystem.actorOf(
 *      //  Tells the handler to forward anything it receives to the Control socket
 *      Props(classOf[GenericSocketMessageHandler], actorLoader, SocketType.Control),
 *
 *      // Creates the Actor with the name of the message type, this allows the Relay to route messages here
 *      name = MessageType.KernelInfoReply.toString
 *   )
 *
 * @param actorLoader The ActorLoader used to load the socket actors
 * @param socketType The type of socket, mapping to an Actor for this class to pass messages along to
 */
class GenericSocketMessageHandler(actorLoader: ActorLoader, socketType: Enumeration#Value)
  extends Actor with LogLike with OrderedSupport {
  override def receive: Receive = {
    case message: KernelMessage => withProcessing {
      logger.debug(s"Sending KernelMessage ${message.header.msg_id} of type " +
        s"${message.header.msg_type} to ${socketType} socket")
      actorLoader.load(socketType) ! message
    }
  }

  /**
   * Defines the types that will be stashed by [[waiting]]
   * while the Actor is in processing state.
   * @return
   */
  override def orderedTypes(): Seq[Class[_]] = Seq(classOf[KernelMessage])
}
