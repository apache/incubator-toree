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

import java.nio.charset.Charset

import akka.actor.{ActorSelection, ActorSystem, ActorRef, Actor}
import akka.util.ByteString
import org.apache.toree.communication.ZMQMessage

//import org.apache.toree.kernel.protocol.v5.kernel.ZMQMessage
import org.apache.toree.kernel.protocol.v5.KernelMessage
import org.apache.toree.kernel.protocol.v5.kernel.Utilities._
import org.apache.toree.utils.MessageLogSupport

/**
 * Represents a generic socket geared toward two-way communication using
 * ZeroMQ and KernelMessage structures.
 * @param actorSocketFunc The function used to retrieve the actor for outgoing
 *                        communication via sockets
 * @param actorForwardFunc The function used to retrieve the actor for incoming
 *                         kernel messages
 */
abstract class ZeromqKernelMessageSocket(
  actorSocketFunc: (ActorSystem, ActorRef) => ActorRef,
  actorForwardFunc: () => ActorSelection
) extends Actor with MessageLogSupport {
  val actorSocketRef = actorSocketFunc(context.system, self)
  val actorForwardRef = actorForwardFunc()

  override def receive: Receive = {
    case message: ZMQMessage =>
      val kernelMessage: KernelMessage = message
      logMessage(kernelMessage)

      // Grab the strings to use for signature verification
      val zmqStrings = message.frames.map((byteString: ByteString) =>
        new String(byteString.toArray, Charset.forName("UTF-8"))
      ).takeRight(4) // TODO: This assumes NO extra buffers, refactor?

      // Forward along our message (along with the strings used for
      // signatures)
      actorForwardRef ! ((zmqStrings, kernelMessage))

    case message: KernelMessage =>
      val zmqMessage: ZMQMessage = message
      logMessage(message)
      actorSocketRef ! zmqMessage
  }
}
