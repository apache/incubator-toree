/*
 * Copyright 2014 IBM Corp.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.ibm.spark.kernel.protocol.v5.handler

import com.ibm.spark.comm.{CommStorage, CommRegistrar, CommWriter}
import com.ibm.spark.kernel.protocol.v5.content.CommOpen
import com.ibm.spark.kernel.protocol.v5.{KMBuilder, Utilities, KernelMessage, ActorLoader}
import com.ibm.spark.utils.MessageLogSupport
import play.api.data.validation.ValidationError
import play.api.libs.json.JsPath

import scala.concurrent.Future
import scala.concurrent.future
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
  override def process(kernelMessage: KernelMessage): Future[_] = {
    logKernelMessageAction("Initiating Comm Open for", kernelMessage)
    Utilities.parseAndHandle(
      kernelMessage.contentString,
      CommOpen.commOpenReads,
      handler = handleCommOpen,
      errHandler = handleParseError
    )
  }

  private def handleCommOpen(commOpen: CommOpen) = future {
    val commId = commOpen.comm_id
    val targetName = commOpen.target_name
    val data = commOpen.data

    logger.debug(
      s"Received comm_open for target '$targetName' with id '$commId'")

    // TODO: Should we be reusing something from the KernelMessage?
    val commWriter = new CommWriter(actorLoader, KMBuilder(), commId)

    if (commStorage.contains(targetName)) {
      logger.debug(s"Executing open callbacks for id '$commId'")

      // TODO: Should we be checking the return values? Probably not.
      commStorage(targetName).executeOpenCallbacks(
        commWriter, commId, targetName, data)

      // Link the new id to the target
      commRegistrar.link(targetName, commId)
    } else {
      logger.warn(s"Received invalid target for Comm Open: $targetName")

      commWriter.close()
    }
  }

  private def handleParseError(invalid: Seq[(JsPath, Seq[ValidationError])]) =
    future {
      // TODO: Determine proper response for a parse failure
      logger.warn("Parse error for Comm Open! Not responding!")
    }

}

