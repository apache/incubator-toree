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

import org.apache.toree.comm.{CommRegistrar, CommStorage, KernelCommWriter}
import org.apache.toree.kernel.protocol.v5.content.{ShutdownReply, ShutdownRequest, CommOpen}
import org.apache.toree.kernel.protocol.v5.kernel.{ActorLoader, Utilities}
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.security.KernelSecurityManager
import org.apache.toree.utils.MessageLogSupport
import play.api.libs.json.JsonValidationError
import play.api.libs.json.JsPath

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Represents the handler to shutdown the kernel
 *
 * @param actorLoader The actor loader to use for actor communication
 */
class ShutdownHandler(
  actorLoader: ActorLoader
) extends BaseHandler(actorLoader) with MessageLogSupport
{
  override def process(kernelMessage: KernelMessage): Future[_] = Future {
    logKernelMessageAction("Initiating Shutdown request for", kernelMessage)

    val kernelInfo = SparkKernelInfo

    val shutdownReply = ShutdownReply(false)

    val replyHeader = Header(
      java.util.UUID.randomUUID.toString,
      "",
      java.util.UUID.randomUUID.toString,
      ShutdownReply.toTypeString,
      kernelInfo.protocolVersion)

    val kernelResponseMessage = KMBuilder()
      .withIds(kernelMessage.ids)
      .withSignature("")
      .withHeader(replyHeader)
      .withParent(kernelMessage)
      .withContentString(shutdownReply).build

    logger.debug("Attempting graceful shutdown.")
    actorLoader.load(SystemActorType.KernelMessageRelay) ! kernelResponseMessage

    // Instruct security manager that exit should be allowed
    KernelSecurityManager.enableRestrictedExit()

    System.exit(0)
  }

}

