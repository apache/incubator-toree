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

import akka.actor.ActorSelection
import akka.pattern.ask
import org.apache.toree.global.{ExecuteRequestState, ExecutionCounter}
import org.apache.toree.kernel.api.{Kernel, KernelLike}
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content._
import org.apache.toree.kernel.protocol.v5.kernel.{ActorLoader, Utilities}
import org.apache.toree.kernel.protocol.v5.stream.KernelOutputStream
import org.apache.toree.{global => kernelGlobal}
import Utilities._
import org.apache.toree.utils._
import play.api.libs.json.JsonValidationError
import play.api.libs.json.JsPath
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util.{Failure, Success}
import org.apache.toree.plugins.PreRunCell

/**
 * Receives an ExecuteRequest KernelMessage and forwards the ExecuteRequest
 * to the interpreter actor.
 *
 * @param actorLoader The loader to use when needing to retrieve actors for
 *                    code execution and output
 * @param kernel The kernel whose factory methods to use
 */
class ExecuteRequestHandler(
  private val actorLoader: ActorLoader,
  private val kernel: Kernel
) extends BaseHandler(actorLoader) with LogLike {
  override def process(km: KernelMessage): Future[_] = {
    // Mark the message as our new incoming kernel message for execution
    ExecuteRequestState.processIncomingKernelMessage(km)

    val skeletonBuilder = KMBuilder().withParent(km).withIds(km.ids)
    val executionCount = ExecutionCounter.incr(km.header.session)
    val relayActor = actorLoader.load(SystemActorType.KernelMessageRelay)

    def handleExecuteRequest(executeRequest: ExecuteRequest):
                             Future[(ExecuteReply, ExecuteResult)] = {
      //  Send an ExecuteInput to the client saying we are executing something
      val executeInputMessage = skeletonBuilder
        .withHeader(MessageType.Outgoing.ExecuteInput)
        .withContentString(ExecuteInput(executeRequest.code, executionCount)).build

      relayMsg(executeInputMessage, relayActor)

      // Construct our new set of streams
      // TODO: Add support for error streams
      val outputStream = kernel.factory(
        parentMessage = km,
        kmBuilder = skeletonBuilder
      ).newKernelOutputStream()
      val executeFuture = ask(
        actorLoader.load(SystemActorType.ExecuteRequestRelay),
        (executeRequest, km, outputStream)
      ).mapTo[(ExecuteReply, ExecuteResult)]
      
      if (!executeRequest.silent && kernel.pluginManager != null){
        import org.apache.toree.plugins.Implicits._
        kernel.pluginManager.fireEvent(PreRunCell, "outputStream" -> outputStream)
      }

      // Flush the output stream after code execution completes to ensure
      // stream messages are sent prior to idle status messages.
      executeFuture andThen { case result =>
        outputStream.flush()
        result
      } andThen {
        case Success(tuple) =>
          val (executeReply, executeResult) = updateCount(tuple, executionCount)

          if (executeReply.status.equals("error")) {
            // Send an ExecuteReplyError with the result of the code execution to ioPub.error
            val replyError: ExecuteReply = ExecuteReplyError(
              executionCount,
              executeReply.ename,
              executeReply.evalue,
              executeReply.traceback)
            relayErrorMessages(relayActor, replyError, skeletonBuilder)
          } else {
            //  Send an ExecuteReply to the client
            val executeReplyMsg = skeletonBuilder
              .withHeader(MessageType.Outgoing.ExecuteReply)
              .withMetadata(Metadata("status" -> executeReply.status))
              .withContentString(executeReply).build
            relayMsg(executeReplyMsg, relayActor)

            if (executeResult.hasContent) {
              val executeResultMsg = skeletonBuilder
                .withIds(Seq(MessageType.Outgoing.ExecuteResult.toString.getBytes))
                .withHeader(MessageType.Outgoing.ExecuteResult)
                .withContentString(executeResult).build
              relayMsg(executeResultMsg, relayActor)
            }
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
    }

    def parseErrorHandler(invalid: Seq[(JsPath, Seq[JsonValidationError])]) = {
      val errs = invalid.map (e => s"JSPath ${e._1} has error ${e._2}").toList
      logger.error(s"Validation errors when parsing ExecuteRequest: ${errs}")
      val replyError: ExecuteReply = ExecuteReplyError(
        executionCount,
        Option("JsonParseException"),
        Option("Error parsing fields"),
        Option(errs)
      )
      Future { relayErrorMessages(relayActor, replyError, skeletonBuilder) }
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
      .withHeader(MessageType.Outgoing.ExecuteReply)
      .withMetadata(Metadata("status" -> replyError.status))
      .withContentString(replyError).build

    val errorContent: ErrorContent =  ErrorContent(
      replyError.ename.get, replyError.evalue.get, replyError.traceback.get)

    val errorMsg = skeletonBuilder
      .withHeader(MessageType.Outgoing.Error)
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
