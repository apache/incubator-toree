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

import akka.actor.ActorSelection
import akka.pattern.ask
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content._
import com.ibm.spark.kernel.protocol.v5.stream.KernelMessageStream
import com.ibm.spark.kernel.protocol.v5.Utilities._
import com.ibm.spark.utils._
import play.api.data.validation.ValidationError
import play.api.libs.json.JsPath

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util.{Failure, Success}

/**
 * Receives an ExecuteRequest KernelMessage and forwards the ExecuteRequest
 * to the interpreter actor.
 */
class ExecuteRequestHandler(actorLoader: ActorLoader)
  extends BaseHandler(actorLoader) with LogLike
{

  override def process(km: KernelMessage): Future[_] = {

    val skeletonBuilder = KMBuilder().withParent(km)
    val executionCount = ExecutionCounter.incr(km.header.session)
    val relayActor = actorLoader.load(SystemActorType.KernelMessageRelay)

    def handleExecuteRequest(executeRequest: ExecuteRequest):
                             Future[(ExecuteReply, ExecuteResult)] = {
      //  Send an ExecuteInput to the client saying we are executing something
      val executeInputMessage = skeletonBuilder
        .withHeader(MessageType.ExecuteInput)
        .withContentString(ExecuteInput(executeRequest.code, executionCount),
                           ExecuteInput.executeInputWrites).build

      relayMsg(executeInputMessage, relayActor)

      // Construct our new set of streams
      // TODO: Add support for error streams
      val outputStream = new KernelMessageStream(actorLoader, skeletonBuilder)
      val executeFuture = ask(
        actorLoader.load(SystemActorType.ExecuteRequestRelay),
        (executeRequest, outputStream)
      ).mapTo[(ExecuteReply, ExecuteResult)]

      executeFuture.onComplete {
        case Success(tuple) =>
          val (executeReply, executeResult) = updateCount(tuple, executionCount)

          //  Send an ExecuteReply to the client
          val executeReplyMsg = skeletonBuilder
            .withHeader(MessageType.ExecuteReply)
            .withContentString(executeReply, ExecuteReply.executeReplyOkWrites)
            .build
          relayMsg(executeReplyMsg, relayActor)

          //  Send an ExecuteResult with the result of the code execution
          if (executeResult.hasContent) {
            val executeResultMsg = skeletonBuilder
              .withIds(Seq(MessageType.ExecuteResult.toString))
              .withHeader(MessageType.ExecuteResult)
              .withContentString(executeResult, ExecuteResult.executeResultWrites)
              .build
            relayMsg(executeResultMsg, relayActor)
          }

        case Failure(error: Throwable) =>
          //  Send an ExecuteReplyError to the client on the Shell socket
          val replyError: ExecuteReply = ExecuteReplyError(
            executionCount,
            Option(error.getClass.getCanonicalName),
            Option(error.getMessage),
            Option(error.getStackTrace.map(_.toString).toList))
          relayErrorMessages(relayActor, replyError, skeletonBuilder)
      }
      executeFuture
    }

    def parseErrorHandler(invalid: Seq[(JsPath, Seq[ValidationError])]) = {
      val errs = invalid.map (e => s"JSPath ${e._1} has error ${e._2}").toList
      logger.error(s"Validation errors when parsing ExecuteRequest: ${errs}")
      val replyError: ExecuteReply = ExecuteReplyError(
        executionCount,
        Option("JsonParseException"),
        Option("Error parsing fields"),
        Option(errs)
      )
      future { relayErrorMessages(relayActor, replyError, skeletonBuilder) }
    }

    Utilities.parseAndHandle(
      km.contentString,
      ExecuteRequest.executeRequestReads,
      handler    = handleExecuteRequest,
      errHandler = parseErrorHandler)
  }

  private def updateCount(tuple: (ExecuteReply, ExecuteResult), n: Int) =
    (tuple._1.copy(execution_count = n), tuple._2.copy(execution_count = n))

  /**
   * Sends an ExecuteReplyError and and Error message to the given actor.
   * @param relayActor The relay to send kernelMessages through
   * @param replyError The reply error to build the error kernelMessages from.
   * @param skeletonBuilder A builder with common base KernelMessage parameters.
   */
  def relayErrorMessages(relayActor: ActorSelection,
                         replyError: ExecuteReply,
                         skeletonBuilder: KMBuilder) {
    val executeReplyMsg = skeletonBuilder
      .withHeader(MessageType.ExecuteReply)
      .withContentString(replyError).build

    val errorContent: ErrorContent =  ErrorContent(
      replyError.ename.get, replyError.evalue.get, replyError.traceback.get)

    val errorMsg = skeletonBuilder
      .withHeader(MessageType.Error)
      .withContentString(errorContent).build

    relayMsg(executeReplyMsg, relayActor)
    //  Send the Error to the client on the IOPub socket
    relayMsg(errorMsg, relayActor)
  }

  private def relayMsg(km: KernelMessage, relayActor: ActorSelection) = {
    logKernelMessageAction("Sending to KernelMessageRelay.", km)
    relayActor ! km
  }
}