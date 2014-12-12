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
  def process(kernelMessage: KernelMessage): Future[_] = {
      val executionCount = ExecutionCounter.incr(kernelMessage.header.session)
      val relayActor = actorLoader.load(SystemActorType.KernelMessageRelay)
      //  This is a collection of common pieces that will be sent back in all reply kernelMessage, use with .copy
      val kernelMessageReplySkeleton =  new KernelMessage( kernelMessage.ids, "", null,  kernelMessage.header, Metadata(), null)

    // TODO refactor using a function like the client's Utilities.parseAndHandle
      Json.parse(kernelMessage.contentString).validate[ExecuteRequest].fold(
        (invalid: Seq[(JsPath, Seq[ValidationError])]) => {
          val validationErrors: List[String] = List()
          for (path <- invalid) {
            validationErrors :+ s"JSPath ${path._1} has error: ${path._2(0).toString}"
          }
          logger.error("Validation errors when parsing ExecuteRequest:")
          logger.error(s"$validationErrors")
          val replyError: ExecuteReply = ExecuteReplyError(
            executionCount, Option("JsonParseException"), Option("Error parsing fields"),
            Option(validationErrors)
          )
          future {
            relayErrorMessages(relayActor, replyError, kernelMessage.header, kernelMessageReplySkeleton)
          }
        },
        (executeRequest: ExecuteRequest) => {
          //  Send a kernelMessage to the clients saying we are executing something
          val executeInputMessage = kernelMessageReplySkeleton.copy(
            //header = kernelMessage.header.copy(msg_type = MessageType.ExecuteInput.toString),
            header = HeaderBuilder.create(MessageType.ExecuteInput.toString),
            contentString = Json.toJson(new ExecuteInput(executeRequest.code, executionCount)).toString()
          )
          relayActor ! executeInputMessage

          // Construct our new set of streams
          val newOutputStream =
            new KernelMessageStream(actorLoader, kernelMessageReplySkeleton)

          // TODO: Add support for error streams
          val executeFuture = ask(
            actorLoader.load(SystemActorType.ExecuteRequestRelay),
            (executeRequest, newOutputStream)
          ).mapTo[(ExecuteReply, ExecuteResult)]

          executeFuture.onComplete {
            case Success(tuple) =>
              logger.debug("Sending Kernel kernelMessages to router")

              //  Send the reply kernelMessage to the client
              val kernelReplyMessage = kernelMessageReplySkeleton.copy(
                //header = kernelMessage.header.copy(msg_type = MessageType.ExecuteReply.toString),
                header = HeaderBuilder.create(MessageType.ExecuteReply.toString),
                contentString = Json.toJson(tuple._1.copy(execution_count = executionCount)).toString()
              )
              relayActor ! kernelReplyMessage

              //  Send the result of the code execution
              if (tuple._2.hasContent) {
                val kernelResultMessage = kernelMessageReplySkeleton.copy(
                  ids = Seq(MessageType.ExecuteResult.toString),
                  //header = kernelMessage.header.copy(msg_type = MessageType.ExecuteResult.toString),
                  header = HeaderBuilder.create(MessageType.ExecuteResult.toString),
                  contentString = Json.toJson(tuple._2.copy(execution_count = executionCount)).toString()
                )
                relayActor ! kernelResultMessage
              }

            case Failure(error: Throwable) =>
              //  Send the error to the client on the Shell socket
              val replyError: ExecuteReply = ExecuteReplyError(
                executionCount, Option(error.getClass.getCanonicalName), Option(error.getMessage),
                Option(error.getStackTrace.map(_.toString).toList)
              )
              relayErrorMessages(relayActor, replyError, kernelMessage.header, kernelMessageReplySkeleton)
          }

          executeFuture
        }
      )
  }


  /**
   * Create a common method to relay errors based on a ExecuteReplyError
   * @param relayActor The relay to send kernelMessages through
   * @param replyError The reply error to build the error kernelMessages from
   * @param headerSkeleton A skeleton for the reply headers (Everything should be filled in except msg_type)
   * @param kernelMessageReplySkeleton A skeleton to build kernelMessages from (Everything should be filled in except header, contentString)
   */
  def relayErrorMessages(relayActor: ActorSelection, replyError: ExecuteReply, headerSkeleton: Header,
                         kernelMessageReplySkeleton: KernelMessage) {
    relayActor ! kernelMessageReplySkeleton.copy(
      //header = headerSkeleton.copy(msg_type = MessageType.ExecuteReply.toString),
      header = HeaderBuilder.create(MessageType.ExecuteReply.toString),
      contentString = replyError
    )

    //  Send the error to the client on the IOPub socket
    val errorContent: ErrorContent =  ErrorContent(
      replyError.ename.get, replyError.evalue.get, replyError.traceback.get
    )

    relayActor ! kernelMessageReplySkeleton.copy(
      //header = headerSkeleton.copy(msg_type = MessageType.Error.toString),
      header = HeaderBuilder.create(MessageType.Error.toString),
      contentString = errorContent
    )
  }
}