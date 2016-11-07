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

import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content.KernelInfoReply
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.utils.LogLike

import scala.concurrent._

/**
 * Receives a KernelInfoRequest KernelMessage and returns a KernelInfoReply
 * KernelMessage.
 */
class KernelInfoRequestHandler(actorLoader: ActorLoader, languageInfo: LanguageInfo)
  extends BaseHandler(actorLoader) with LogLike
{
  def process(kernelMessage: KernelMessage): Future[_] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      logger.debug("Sending kernel info reply message")

      val kernelInfo = SparkKernelInfo
      val kernelInfoReply = KernelInfoReply(
        kernelInfo.protocolVersion,
        kernelInfo.implementation,
        kernelInfo.implementationVersion,
        languageInfo,
        kernelInfo.banner
      )

      // TODO could we use HeaderBuilder here?
      val replyHeader = Header(
        java.util.UUID.randomUUID.toString,
        "",
        java.util.UUID.randomUUID.toString,
        MessageType.Outgoing.KernelInfoReply.toString,
        kernelInfo.protocolVersion
      )

      val kernelResponseMessage = KMBuilder()
        .withIds(kernelMessage.ids)
        .withSignature("")
        .withHeader(replyHeader)
        .withParent(kernelMessage)
        .withContentString(kernelInfoReply).build

      actorLoader.load(SystemActorType.KernelMessageRelay) ! kernelResponseMessage
    }
  }
}
