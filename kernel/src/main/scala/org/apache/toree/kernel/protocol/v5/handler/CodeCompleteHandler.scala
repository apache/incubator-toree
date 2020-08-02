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

class CodeCompleteHandler(actorLoader: ActorLoader)
  extends BaseHandler(actorLoader) with MessageLogSupport
{
  override def process(kernelMessage: KernelMessage): Future[_] = {
    logKernelMessageAction("Generating code completion for", kernelMessage)
    Utilities.parseAndHandle(
      kernelMessage.contentString,
      CompleteRequest.completeRequestReads,
      completeRequest(kernelMessage, _ : CompleteRequest)
    )
  }

  private def completeRequest(km: KernelMessage, cr: CompleteRequest):
                              Future[(Int, List[String])] = {
    val interpreterActor = actorLoader.load(SystemActorType.Interpreter)
    val codeCompleteFuture = ask(interpreterActor, cr).mapTo[(Int, List[String])]
    codeCompleteFuture.onComplete {
      case Success(tuple) =>
        val reply = CompleteReplyOk(tuple._2, tuple._1,
                                    cr.cursor_pos, Metadata())
        val completeReplyType = MessageType.Outgoing.CompleteReply.toString
        logKernelMessageAction("Sending code complete reply for", km)
        actorLoader.load(SystemActorType.KernelMessageRelay) !
          km.copy(
            header = HeaderBuilder.create(completeReplyType),
            parentHeader = km.header,
            contentString = Json.toJson(reply).toString
          )
      case _ =>
        new Exception("Parse error in CodeCompleteHandler")
    }
    codeCompleteFuture
  }
}
