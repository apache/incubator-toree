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

import org.apache.toree.comm.{KernelCommWriter, CommStorage, CommRegistrar, CommWriter}
import org.apache.toree.global.ExecuteRequestState
import org.apache.toree.kernel.protocol.v5.content.CommOpen
import org.apache.toree.kernel.protocol.v5.kernel.{Utilities, ActorLoader}
import org.apache.toree.kernel.protocol.v5.{KMBuilder, KernelMessage}
import org.apache.toree.utils.MessageLogSupport
import play.api.libs.json.JsonValidationError
import play.api.libs.json.JsPath

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Represents the handler for comm_open messages.
 *
 * @param actorLoader The actor loader to use for actor communication
 * @param commRegistrar The Comm registrar used for linking
 * @param commStorage The Comm storage used for open callbacks
 */
class CommOpenHandler(
  actorLoader: ActorLoader, commRegistrar: CommRegistrar,
  commStorage: CommStorage
) extends BaseHandler(actorLoader) with MessageLogSupport
{
  override def process(kernelMessage: KernelMessage): Future[_] = Future {
    logKernelMessageAction("Initiating Comm Open for", kernelMessage)

    ExecuteRequestState.processIncomingKernelMessage(kernelMessage)

    val kmBuilder = KMBuilder().withParent(kernelMessage)

    Utilities.parseAndHandle(
      kernelMessage.contentString,
      CommOpen.commOpenReads,
      handler = handleCommOpen(kmBuilder),
      errHandler = handleParseError
    )
  }

  private def handleCommOpen(kmBuilder: KMBuilder)(commOpen: CommOpen) = {
    val commId = commOpen.comm_id
    val targetName = commOpen.target_name
    val data = commOpen.data

    logger.debug(
      s"Received comm_open for target '$targetName' with id '$commId'")

    val commWriter = new KernelCommWriter(actorLoader, kmBuilder, commId)

    commStorage.getTargetCallbacks(targetName) match {
      case None             =>
        logger.warn(s"Received invalid target for Comm Open: $targetName")

        commWriter.close()
      case Some(callbacks)  =>
        logger.debug(s"Executing open callbacks for id '$commId'")

        // TODO: Should we be checking the return values? Probably not.
        callbacks.executeOpenCallbacks(commWriter, commId, targetName, data)
          .filter(_.isFailure).map(_.failed).foreach(throwable => {
            logger.error("Comm open callback encountered an error!", throwable)
          })
    }
  }

  private def handleParseError(invalid: Seq[(JsPath, Seq[JsonValidationError])]) = {
    // TODO: Determine proper response for a parse failure
    logger.warn("Parse error for Comm Open! Not responding!")
  }

}

