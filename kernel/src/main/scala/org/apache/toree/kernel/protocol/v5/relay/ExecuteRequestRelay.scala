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

package org.apache.toree.kernel.protocol.v5.relay

import java.io.OutputStream
import akka.actor.Actor
import akka.pattern._
import akka.util.Timeout
import org.apache.toree.interpreter.{ExecuteAborted, ExecuteError, ExecuteFailure, ExecuteOutput}
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content._
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.kernel.protocol.v5.magic.MagicParser
import org.apache.toree.plugins.PluginManager
import org.apache.toree.utils.LogLike
import scala.concurrent.Future
import scala.concurrent.duration._
import org.apache.toree.plugins.NewOutputStream

case class ExecuteRequestRelay(
  actorLoader: ActorLoader,
  pluginManager: PluginManager,
  magicParser: MagicParser
)
  extends Actor with LogLike
{
  import context._
  implicit val timeout = Timeout(21474835.seconds)

  /**
   * Takes an ExecuteFailure and (ExecuteReply, ExecuteResult) with contents
   * dictated by the type of failure (either an error or an abort).
   *
   * @param failure the failure
   * @return (ExecuteReply, ExecuteResult)
   */
  private def failureMatch(failure: ExecuteFailure) =
    failure match {
      case err: ExecuteError =>
        val error = ExecuteReplyError(
          1, Some(err.name), Some(err.value), Some(err.stackTrace)
        )
        val result =
          ExecuteResult(1, Data(MIMEType.PlainText -> err.toString), Metadata())
        (error, result)

      case _: ExecuteAborted =>
        val abort = ExecuteReplyAbort(1)
        val result = ExecuteResult(1, Data(), Metadata())
        (abort, result)
    }

  /**
   * Packages the response into an ExecuteReply,ExecuteResult tuple.
   *
   * @param future The future containing either the output or failure
   * @return The tuple representing the proper response
   */
  private def packageFutureResponse(
    future: Future[Either[ExecuteOutput, ExecuteFailure]]
  ): Future[(ExecuteReply, ExecuteResult)] = future.map { value =>
    if (value.isLeft) {
      val data = value.left.get
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
      val interpreterActor = actorLoader.load(SystemActorType.Interpreter)

      // Store our old sender so we don't lose it in the callback
      // NOTE: Should point back to our KernelMessageRelay
      val oldSender = sender()

      // Sets the outputStream for this particular ExecuteRequest
      import org.apache.toree.plugins.Implicits._
      pluginManager.fireEventFirstResult(
        NewOutputStream,
        "outputStream" -> outputStream
      )

      // Parse the code for magics before sending it to the interpreter and
      // pipe the response to sender
      (magicParser.parse(executeRequest.code) match {
        case Left(code) =>
          val parsedRequest =
            (executeRequest.copy(code = code), parentMessage, outputStream)
          val interpreterFuture = (interpreterActor ? parsedRequest)
            .mapTo[Either[ExecuteOutput, ExecuteFailure]]
          packageFutureResponse(interpreterFuture)

        case Right(error) =>
          val failure = ExecuteError("Error parsing magics!", error, Nil)
          Future { failureMatch(failure) }
      }) pipeTo oldSender
  }
}
