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

import akka.pattern.ask
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content._
import org.apache.toree.kernel.protocol.v5.kernel.{ActorLoader, Utilities}
import Utilities._
import org.apache.toree.utils.{MessageLogSupport, LogLike}
import play.api.libs.json.JsonValidationError
import play.api.libs.json.{JsPath, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

class IsCompleteHandler(actorLoader: ActorLoader)
  extends BaseHandler(actorLoader) with MessageLogSupport
{
  override def process(kernelMessage: KernelMessage): Future[_] = {
    logKernelMessageAction("Determining if code is complete for", kernelMessage)
    Utilities.parseAndHandle(
      kernelMessage.contentString,
      IsCompleteRequest.isCompleteRequestReads,
      isCompleteRequest(kernelMessage, _ : IsCompleteRequest)
    )
  }

  private def isCompleteRequest(km: KernelMessage, cr: IsCompleteRequest):
  Future[(String, String)] = {
    val interpreterActor = actorLoader.load(SystemActorType.Interpreter)
    val codeCompleteFuture = ask(interpreterActor, cr).mapTo[(String, String)]
    codeCompleteFuture.onComplete {
      case Success(tuple) =>
        val reply = IsCompleteReply(tuple._1, tuple._2)
        val isCompleteReplyType = MessageType.Outgoing.IsCompleteReply.toString
        logKernelMessageAction("Sending is complete reply for", km)
        actorLoader.load(SystemActorType.KernelMessageRelay) !
          km.copy(
            header = HeaderBuilder.create(isCompleteReplyType),
            parentHeader = km.header,
            contentString = Json.toJson(reply).toString
          )
      case _ =>
        new Exception("Parse error in CodeCompleteHandler")
    }
    codeCompleteFuture
  }
}
