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

package com.ibm.spark.kernel.protocol.v5.relay

import java.io.OutputStream

import akka.actor.Actor
import akka.pattern._
import akka.util.Timeout
import com.ibm.spark.interpreter.{ExecuteAborted, ExecuteError, ExecuteFailure, ExecuteOutput}
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content._
import com.ibm.spark.kernel.protocol.v5.kernel.ActorLoader
import com.ibm.spark.kernel.protocol.v5.magic.{ValidateMagicMessage, ExecuteMagicMessage}
import com.ibm.spark.magic.MagicOutput
import com.ibm.spark.utils.LogLike

import scala.concurrent.Future
import scala.concurrent.duration._

case class ExecuteRequestRelay(actorLoader: ActorLoader)
  extends Actor with LogLike
{
  import context._
  implicit val timeout = Timeout(100000.days)

  private def failureMatch(failure: ExecuteFailure) =
    failure match {
      case error: ExecuteError =>
        (
          ExecuteReplyError(1, Some(error.name), Some(error.value),
            Some(error.stackTrace)),
          ExecuteResult(1, Data(MIMEType.PlainText -> error.toString), Metadata())
          )
      case _: ExecuteAborted =>
        (ExecuteReplyAbort(1), ExecuteResult(1, Data(), Metadata()))
    }

  /**
   * Packages the response into an ExecuteReply,ExecuteResult tuple.
   * @param future The future containing either the output or failure
   * @return The tuple representing the proper response
   */
  private def packageFutureResponse(
    future: Future[Either[AnyRef, ExecuteFailure]]
  ) = future.map { value =>
    if (value.isLeft) {
      val output = value.left.get
      val data = output match {
        case out: MagicOutput => out
        case out: ExecuteOutput => Data(MIMEType.PlainText -> out)
      }
      (
        ExecuteReplyOk(1, Some(Payloads()), Some(UserExpressions())),
        ExecuteResult(1, data, Metadata())
      )
    } else {
      failureMatch(value.right.get)
    }
  }

  override def receive: Receive = {
    case (executeRequest: ExecuteRequest, parentMessage: KernelMessage,
      outputStream: OutputStream) =>
      val magicManager = actorLoader.load(SystemActorType.MagicManager)
      val futureIsMagic =
        magicManager ? ValidateMagicMessage(executeRequest.code)

      // Store our old sender so we don't lose it in the callback
      // NOTE: Should point back to our KernelMessageRelay
      val oldSender = sender

      // TODO: Investigate how to use the success to set the future ask
      //       destination without running duplicate code
      futureIsMagic.onSuccess {
        case true =>
          val magicMessage = ExecuteMagicMessage(executeRequest.code)
          val future  =
            (magicManager ? ((magicMessage, outputStream)))
              .mapTo[Either[MagicOutput, ExecuteFailure]]

          packageFutureResponse(future) pipeTo oldSender
        case false =>
          val interpreterActor = actorLoader.load(SystemActorType.Interpreter)
          val future =
            (interpreterActor ? ((executeRequest, parentMessage, outputStream)))
              .mapTo[Either[ExecuteOutput, ExecuteFailure]]

          packageFutureResponse(future) pipeTo oldSender
      }
  }
}
