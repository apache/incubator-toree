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

package org.apache.toree.kernel.api

import org.apache.toree.kernel.protocol.v5
import org.apache.toree.kernel.protocol.v5.KernelMessage
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader

/**
 * Represents the methods available to stream data from the kernel to the
 * client.
 */
class StreamMethods(actorLoader: ActorLoader, parentMessage: KernelMessage)
  extends StreamMethodsLike
{
  private[api] val kmBuilder = v5.KMBuilder()
    .withParent(parentMessage)
    .withIds(Seq(v5.content.StreamContent.toTypeString.getBytes))
    .withHeader(v5.content.StreamContent.toTypeString)

  /**
   * Sends all text provided as one stream message to the client.
   * @param text The text to wrap in a stream message
   */
  override def sendAll(text: String): Unit = {
    val streamContent = v5.content.StreamContent(
      "stdout", text
    )

    val kernelMessage = kmBuilder.withContentString(streamContent).build

    actorLoader.load(v5.SystemActorType.KernelMessageRelay) ! kernelMessage
  }
}
