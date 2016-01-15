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

import akka.actor.Actor
import akka.util.ByteString
import org.apache.toree.communication.ZMQMessage
import org.apache.toree.utils.LogLike

/**
 * The server endpoint for heartbeat messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class Heartbeat(socketFactory : SocketFactory) extends Actor with LogLike {
  logger.debug("Created new Heartbeat actor")
  val socket = socketFactory.Heartbeat(context.system, self)

  override def receive: Receive = {
    case message: ZMQMessage =>
      logger.trace("Heartbeat received message: " +
        message.frames.map((byteString: ByteString) =>
          new String(byteString.toArray)).mkString("\n"))
      socket ! message
  }
}