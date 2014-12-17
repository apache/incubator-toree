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
import play.api.libs.json.{JsPath, Json}

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
    val executionCount = ExecutionCounter.incr(km.header.session)
    val relayActor = actorLoader.load(SystemActorType.KernelMessageRelay)
    val replySkeleton = KernelMessage(km.ids, "", null, km.header, Metadata(), null)

    def handleExecuteRequest(executeRequest: ExecuteRequest):
                             Future[(ExecuteReply, ExecuteResult)] = {
      //  Send an ExecuteInput to the client saying we are executing something
      val executeInputMessage = replySkeleton.copy(
        header = HeaderBuilder.create(MessageType.ExecuteInput.toString),
        contentString = Json.toJson(
          ExecuteInput(executeRequest.code, executionCount)).toString)
      relayMsg(executeInputMessage, relayActor)

      // Construct our new set of streams
      // TODO: Add support for error streams
      val outputStream = new KernelMessageStream(actorLoader, replySkeleton)
      val executeFuture = ask(
        actorLoader.load(SystemActorType.ExecuteRequestRelay),
        (executeRequest, outputStream)
      ).mapTo[(ExecuteReply, ExecuteResult)]

      executeFuture.onComplete {
        case Success(replyResultTuple) =>
          //  Send an ExecuteReply to the client
          val executeReplyMsg = replySkeleton.copy(
            header = HeaderBuilder.create(MessageType.ExecuteReply.toString),
            contentString = Json.toJson(
              replyResultTuple._1.copy(execution_count = executionCount)).toString)
          relayMsg(executeReplyMsg, relayActor)

          //  Send an ExecuteResult with the result of the code execution
          if (replyResultTuple._2.hasContent) {
            val executeResultMsg = replySkeleton.copy(
              ids = Seq(MessageType.ExecuteResult.toString),
              header = HeaderBuilder.create(MessageType.ExecuteResult.toString),
              contentString = Json.toJson(
                replyResultTuple._2.copy(execution_count = executionCount)).toString)
            relayMsg(executeResultMsg, relayActor)
          }

        case Failure(error: Throwable) =>
          //  Send an ExecuteReplyError to the client on the Shell socket
          val replyError: ExecuteReply = ExecuteReplyError(
            executionCount,
            Option(error.getClass.getCanonicalName),
            Option(error.getMessage),
            Option(error.getStackTrace.map(_.toString).toList))
          relayErrorMessages(relayActor, replyError, km.header, replySkeleton)
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
      future { relayErrorMessages(relayActor, replyError, km.header, replySkeleton) }
    }

    Utilities.parseAndHandle(
      km.contentString,
      ExecuteRequest.executeRequestReads,
      handler    = handleExecuteRequest,
      errHandler = parseErrorHandler)
  }

  /**
   * Sends an ExecuteReplyError and and Error message to the given actor.
   * @param relayActor The relay to send kernelMessages through
   * @param replyError The reply error to build the error kernelMessages from
   * @param headerSkeleton A skeleton for the reply headers. Everything should
   *                       be filled in except msg_type.
   * @param replySkeleton A skeleton to build kernelMessages from. Everything
   *                      should be filled in except header, contentString.
   */
  def relayErrorMessages(relayActor: ActorSelection,
                         replyError: ExecuteReply,
                         headerSkeleton: Header,
                         replySkeleton: KernelMessage) {
    val executeReplyMsg = replySkeleton.copy(
      header = HeaderBuilder.create(MessageType.ExecuteReply.toString),
      contentString = replyError)

    val errorContent: ErrorContent =  ErrorContent(
      replyError.ename.get, replyError.evalue.get, replyError.traceback.get)

    val errorMsg = replySkeleton.copy(
      header = HeaderBuilder.create(MessageType.Error.toString),
      contentString = errorContent)

    relayMsg(executeReplyMsg, relayActor)
    //  Send the Error to the client on the IOPub socket
    relayMsg(errorMsg, relayActor)
  }

  private def relayMsg(km: KernelMessage, relayActor: ActorSelection) = {
    logKernelMessageAction("Sending to KernelMessageRelay.", km)
    relayActor ! km
  }
}