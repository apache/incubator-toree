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

import java.nio.charset.Charset

import akka.actor.Actor
import akka.util.ByteString
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5.kernel.{ActorLoader, Utilities}
import com.ibm.spark.kernel.protocol.v5.{KernelMessage, SystemActorType}
import Utilities._
import com.ibm.spark.utils.{MessageLogSupport, LogLike}

/**
 * The server endpoint for shell messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class Shell(socketFactory: SocketFactory, actorLoader: ActorLoader)
  extends Actor with MessageLogSupport{
  logger.trace("Created new Shell actor")
  val socket = socketFactory.Shell(context.system, self)
  override def receive: Receive = {
    case message: ZMQMessage =>
      val kernelMessage: KernelMessage = message
      logMessage(kernelMessage)

      // Grab the strings to use for signature verification
      val zmqStrings = message.frames.map((byteString: ByteString) =>
        new String(byteString.toArray, Charset.forName("UTF-8"))
      ).takeRight(4) // TODO: This assumes NO extra buffers, refactor?

      // Forward along our message (along with the strings used for signatures)
      actorLoader.load(SystemActorType.KernelMessageRelay) !
        ((zmqStrings, kernelMessage))

    case message: KernelMessage =>
      val zmqMessage: ZMQMessage = message
      logMessage(message)
      socket ! zmqMessage
  }
}
