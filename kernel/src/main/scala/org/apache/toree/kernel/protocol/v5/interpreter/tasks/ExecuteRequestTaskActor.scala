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

package org.apache.toree.kernel.protocol.v5.interpreter.tasks

import java.io.OutputStream

import akka.actor.{Actor, Props}
import org.apache.toree.global.StreamState
import org.apache.toree.interpreter.{ExecuteAborted, ExecuteError, Interpreter, Results}
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content._
import org.apache.toree.security.KernelSecurityManager
import org.apache.toree.utils.{ConditionalOutputStream, LogLike, MultiOutputStream}

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

        // Update our global streams to be used by future output
        // NOTE: This is not async-safe! This is expected to be broken when
        //       running asynchronously! Use an alternative for data
        //       communication!
        StreamState.setStreams(newInputStream, newOutputStream, newErrorStream)

        val (success, result) = {
            // Add our parent message with StreamInfo type included
//            interpreter.doQuietly {
//              interpreter.bind(
//                "$streamInfo",
//                "org.apache.toree.kernel.api.StreamInfo",
//                new KernelMessage(
//                  ids = parentMessage.ids,
//                  signature = parentMessage.signature,
//                  header = parentMessage.header,
//                  parentHeader = parentMessage.parentHeader,
//                  metadata = parentMessage.metadata,
//                  contentString = parentMessage.contentString
//                ) with StreamInfo,
//                List( """@transient""", """implicit""")
//              )
              // TODO: Think of a cleaner wrapper to handle updating the Console
              //       input and output streams
//              interpreter.interpret(
//                """val $updateOutput = {
//                Console.setIn(System.in)
//                Console.setOut(System.out)
//                Console.setErr(System.err)
//              }""".trim)
//            }
            interpreter.interpret(executeRequest.code.trim, outputStreamResult = Some(outputStream))
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
