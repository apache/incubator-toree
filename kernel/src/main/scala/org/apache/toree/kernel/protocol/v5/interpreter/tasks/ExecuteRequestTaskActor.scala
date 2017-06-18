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
import java.util.concurrent.TimeUnit
import akka.actor.{Actor, Props}
import org.apache.toree.global
import org.apache.toree.global.StreamState
import org.apache.toree.interpreter.{ExecuteAborted, ExecuteError, Interpreter, Results}
import org.apache.toree.kernel.api.KernelLike
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content._
import org.apache.toree.security.KernelSecurityManager
import org.apache.toree.utils.{ConditionalOutputStream, LogLike, MultiOutputStream}

object ExecuteRequestTaskActor {
  def props(kernel: KernelLike, interpreter: Interpreter): Props =
    Props(classOf[ExecuteRequestTaskActor], kernel, interpreter)
}

class ExecuteRequestTaskActor(
    kernel: KernelLike,
    interpreter: Interpreter) extends Actor with LogLike {

  require(interpreter != null)

  private var shutdownMonitorTaskId: Option[String] = None
  @volatile private var interpreterRunning: Boolean = false
  @volatile private var lastExecuteRequest: Long = System.currentTimeMillis

  override def preStart(): Unit = {
    shutdownMonitorTaskId = if (kernel.config != null && kernel.config.hasPath("idle_timeout")) {
      val timeoutMs = kernel.config.getDuration("idle_timeout", TimeUnit.MILLISECONDS)
      Some(global.ScheduledTaskManager.instance.addTask(timeInterval = 30000, task = {
        val now = System.currentTimeMillis
        if (!interpreterRunning && (System.currentTimeMillis - lastExecuteRequest > timeoutMs)) {
          val msg = s"No execution requests in ${timeoutMs / 1000} seconds, shutting down."
          println(msg)
          logger.info(msg)
          kernel.shutdown()
        }
      }))
    } else {
      None
    }
  }

  override def postStop(): Unit = {
    shutdownMonitorTaskId match {
      case Some(taskId) =>
        global.ScheduledTaskManager.instance.removeTask(taskId)
        shutdownMonitorTaskId = None
      case _ =>
    }
  }

  override def receive: Receive = {
    case (executeRequest: ExecuteRequest, parentMessage: KernelMessage,
      outputStream: OutputStream) =>
      // If the cell is not empty, then interpret.
      executeRequest.code.trim match {
        case "quit" | "exit" | ":q" =>
          kernel.shutdown()
          sender ! Left(Map.empty)

        case "" =>
          lastExecuteRequest = System.currentTimeMillis
          // If we get empty code from a cell then just return ExecuteReplyOk
          sender ! Left(Map.empty)

        case _ =>
          interpreterRunning = true

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

          // update the last execution time
          interpreterRunning = false
          lastExecuteRequest = System.currentTimeMillis

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
