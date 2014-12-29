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

package com.ibm.spark.kernel.protocol.v5.interpreter.tasks

import java.io.OutputStream

import akka.actor.{Props, Actor}
import com.ibm.spark.interpreter.{ExecuteAborted, Results, ExecuteError, Interpreter}
import com.ibm.spark.kernel.api.StreamInfo
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content._
import com.ibm.spark.security.KernelSecurityManager
import com.ibm.spark.utils.{ConditionalOutputStream, MultiOutputStream, GlobalStreamState, LogLike}

object ExecuteRequestTaskActor {
  def props(interpreter: Interpreter): Props =
    Props(classOf[ExecuteRequestTaskActor], interpreter)
}

class ExecuteRequestTaskActor(interpreter: Interpreter) extends Actor with LogLike {
  require(interpreter != null)

  override def receive: Receive = {
    case (executeRequest: ExecuteRequest, parentMessage: KernelMessage,
      outputStream: OutputStream) =>
      // If the cell is not empty, then interpret.
      if(executeRequest.code.trim != "") {
        //interpreter.updatePrintStreams(System.in, outputStream, outputStream)
        val newInputStream = System.in
        val newOutputStream = buildOutputStream(outputStream, System.out)
        val newErrorStream = buildOutputStream(outputStream, System.err)

        val (success, result) =
          GlobalStreamState.withStreams(
            newInputStream, newOutputStream, newErrorStream
          ) {
            // Add our parent message with StreamInfo type included
            interpreter.doQuietly {
              interpreter.bind(
                "$streamInfo",
                "com.ibm.spark.kernel.api.StreamInfo",
                new KernelMessage(
                  ids = parentMessage.ids,
                  signature = parentMessage.signature,
                  header = parentMessage.header,
                  parentHeader = parentMessage.parentHeader,
                  metadata = parentMessage.metadata,
                  contentString = parentMessage.contentString
                ) with StreamInfo,
                List( """@transient""", """implicit""")
              )
              // TODO: Think of a cleaner wrapper to handle updating the Console
              //       input and output streams
              interpreter.interpret(
                """
                Console.setIn(System.in)
                Console.setOut(System.out)
                Console.setErr(System.err)
              """)
            }
            interpreter.interpret(executeRequest.code)
          }

        logger.debug(s"Interpreter execution result was ${success}")
        success match {
          case Results.Success =>
            val output = result.left.get
            sender ! Left(output)
          case Results.Error =>
            val error = result.right.get
            sender ! Right(error)
          case Results.Aborted =>
            sender ! Right(new ExecuteAborted)
          case Results.Incomplete =>
            // If we get an incomplete it's most likely a syntax error, so
            // let the user know.
            sender ! Right(new ExecuteError("Syntax Error.", "", List()))
        }
      } else {
        // If we get empty code from a cell then just return ExecuteReplyOk
        sender ! Left("")
      }
    case unknownValue =>
      logger.warn(s"Received unknown message type ${unknownValue}")
      sender ! "Unknown message" // TODO: Provide a failure message type to be passed around?
  }

  private def buildOutputStream(
    newOutput: OutputStream,
    defaultOutput: OutputStream
  ) = {
    def isRestrictedThread = {
      val currentGroup = Thread.currentThread().getThreadGroup
      val restrictedGroupName =
        KernelSecurityManager.RestrictedGroupName

      currentGroup != null && currentGroup.getName == restrictedGroupName
    }

    new MultiOutputStream(List[OutputStream](
      new ConditionalOutputStream(newOutput, isRestrictedThread),
      new ConditionalOutputStream(defaultOutput, !isRestrictedThread)
    ))
  }
}
