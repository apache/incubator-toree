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
import org.apache.toree.kernel.protocol.v5.{KMBuilder, KernelMessage}
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader

/**
 * Represents the methods available to send display content from the kernel to the
 * client.
 */
class DisplayMethods(
  private val actorLoader: ActorLoader,
  private val parentMessage: KernelMessage,
  private val kernelMessageBuilder: KMBuilder)

  extends DisplayMethodsLike {

  private[api] val kmBuilder = kernelMessageBuilder.withParent(parentMessage)

  override def content(mimeType: String, data: String): Unit = {
    val displayData = v5.content.DisplayData("user", Map(mimeType -> data), Map())

    val kernelMessage = kmBuilder
      .withIds(Seq(v5.content.DisplayData.toTypeString.getBytes))
      .withHeader(v5.content.DisplayData.toTypeString)
      .withContentString(displayData).build

    actorLoader.load(v5.SystemActorType.KernelMessageRelay) ! kernelMessage
  }

  override def clear(wait: Boolean = false): Unit = {
    val clearOutput = v5.content.ClearOutput(wait)

    val kernelMessage = kmBuilder
      .withIds(Seq(v5.content.ClearOutput.toTypeString.getBytes))
      .withHeader(v5.content.ClearOutput.toTypeString)
      .withContentString(clearOutput).build

    actorLoader.load(v5.SystemActorType.KernelMessageRelay) ! kernelMessage

  }
}
