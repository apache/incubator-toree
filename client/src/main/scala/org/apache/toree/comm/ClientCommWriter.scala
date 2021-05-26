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

package org.apache.toree.comm

import org.apache.toree.annotations.Experimental
import org.apache.toree.kernel.protocol.v5
import org.apache.toree.kernel.protocol.v5.client.ActorLoader
import org.apache.toree.kernel.protocol.v5.content.{CommClose, CommMsg, CommOpen, CommContent}
import org.apache.toree.kernel.protocol.v5._

/**
 * Represents a CommWriter to send messages from the Client to the Kernel.
 *
 * @param actorLoader The actor loader to use for loading actors responsible for
 *                    communication
 * @param kmBuilder The kernel message builder used to construct kernel messages
 * @param commId The comm id associated with this writer (defaults to a
 *               random UUID)
 */
@Experimental
class ClientCommWriter(
  private val actorLoader: ActorLoader,
  private val kmBuilder: KMBuilder,
  override private[comm] val commId: v5.UUID
) extends CommWriter(commId) {

  /**
   * Sends the comm message (open/msg/close) to the actor responsible for
   * relaying messages.
   *
   * @param commContent The message to relay (will be packaged)
   *
   * @tparam T Either CommOpen, CommMsg, or CommClose
   */
  override protected[comm] def sendCommKernelMessage[
    T <: KernelMessageContent with CommContent
  ](commContent: T): Unit = {
    val messageType = commContent match {
      case _: CommOpen  => CommOpen.toTypeString
      case _: CommMsg   => CommMsg.toTypeString
      case _: CommClose => CommClose.toTypeString
      case _            =>
        throw new Throwable("Invalid kernel message type!")
    }
    println(">>>")
    println(messageType)
    print(commContent)
    actorLoader.load(SocketType.ShellClient) !
      kmBuilder.withHeader(messageType).withContentString(commContent).build
  }
}
