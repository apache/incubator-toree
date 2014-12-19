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

package com.ibm.spark.kernel.protocol.v5.handler

import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.KernelInfoReply
import com.ibm.spark.utils.LogLike
import play.api.libs.json.Json

import scala.concurrent._

/**
 * Receives a KernelInfoRequest KernelMessage and returns a KernelInfoReply
 * KernelMessage.
 */
class KernelInfoRequestHandler(actorLoader: ActorLoader)
  extends BaseHandler(actorLoader) with LogLike
{
  def process(kernelMessage: KernelMessage): Future[_] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    future {
      logger.debug("Sending kernel info reply message")

      val kernelInfo = SparkKernelInfo
      val kernelInfoReply = KernelInfoReply(
        kernelInfo.protocolVersion,
        kernelInfo.implementation,
        kernelInfo.implementationVersion,
        kernelInfo.language,
        kernelInfo.languageVersion,
        kernelInfo.banner
      )

      // TODO could we use HeaderBuilder here?
      val replyHeader = Header(
        java.util.UUID.randomUUID.toString,
        "",
        java.util.UUID.randomUUID.toString,
        MessageType.KernelInfoReply.toString,
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