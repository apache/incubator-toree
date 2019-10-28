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

import org.apache.toree.comm.{KernelCommWriter, CommRegistrar, CommWriter, CommStorage}
import org.apache.toree.global.ExecuteRequestState
import org.apache.toree.kernel.protocol.v5.content.CommMsg
import org.apache.toree.kernel.protocol.v5.kernel.{Utilities, ActorLoader}
import org.apache.toree.kernel.protocol.v5.{KMBuilder, KernelMessage}
import org.apache.toree.utils.MessageLogSupport
import play.api.libs.json.JsonValidationError
import play.api.libs.json.JsPath

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Represents the handler for comm_msg messages.
 *
 * @param actorLoader The actor loader to use for actor communication
 * @param commRegistrar The Comm registrar (unused)
 * @param commStorage The Comm storage used for msg callbacks
 */
class CommMsgHandler(
  actorLoader: ActorLoader, commRegistrar: CommRegistrar,
  commStorage: CommStorage
) extends BaseHandler(actorLoader) with MessageLogSupport
{
  override def process(kernelMessage: KernelMessage): Future[_] = Future {
    logKernelMessageAction("Initiating Comm Msg for", kernelMessage)

    ExecuteRequestState.processIncomingKernelMessage(kernelMessage)

    val kmBuilder = KMBuilder().withParent(kernelMessage)

    Utilities.parseAndHandle(
      kernelMessage.contentString,
      CommMsg.commMsgReads,
      handler = handleCommMsg(kmBuilder),
      errHandler = handleParseError
    )
  }

  private def handleCommMsg(kmBuilder: KMBuilder)(commMsg: CommMsg) = {
    val commId = commMsg.comm_id
    val data = commMsg.data

    logger.debug(s"Received comm_msg with id '$commId'")

    val commWriter = new KernelCommWriter(actorLoader, kmBuilder, commId)

    commStorage.getCommIdCallbacks(commId) match {
      case None             =>
        logger.warn(s"Received invalid id for Comm Msg: $commId")
      case Some(callbacks)  =>
        logger.debug(s"Executing msg callbacks for id '$commId'")

        // TODO: Should we be checking the return values? Probably not.
        callbacks.executeMsgCallbacks(commWriter, commId, data)
          .filter(_.isFailure).map(_.failed).foreach(throwable => {
            logger.error("Comm msg callback encountered an error!", throwable)
          })
    }
  }

  private def handleParseError(invalid: Seq[(JsPath, Seq[JsonValidationError])]) = {
    // TODO: Determine proper response for a parse failure
    logger.warn("Parse error for Comm Msg! Not responding!")
  }

}

