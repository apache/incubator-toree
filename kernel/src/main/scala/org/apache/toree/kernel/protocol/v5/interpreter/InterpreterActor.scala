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

package com.ibm.spark.kernel.protocol.v5.interpreter

import java.io.OutputStream

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.ibm.spark.interpreter.Interpreter
import com.ibm.spark.kernel.protocol.v5.KernelMessage
import com.ibm.spark.kernel.protocol.v5.interpreter.tasks._
import com.ibm.spark.kernel.protocol.v5.content._
import com.ibm.spark.interpreter._
import com.ibm.spark.utils.LogLike

import scala.concurrent.duration._

object InterpreterActor {
  def props(interpreter: Interpreter): Props =
    Props(classOf[InterpreterActor], interpreter)
}

// TODO: Investigate restart sequence
//
// http://doc.akka.io/docs/akka/2.2.3/general/supervision.html
//
// "create new actor instance by invoking the originally provided factory again"
//
// Does this mean that the interpreter instance is not gc and is passed in?
//
class InterpreterActor(
  interpreterTaskFactory: InterpreterTaskFactory
) extends Actor with LogLike {
  // NOTE: Required to provide the execution context for futures with akka
  import context._

  // NOTE: Required for ask (?) to function... maybe can define elsewhere?
  implicit val timeout = Timeout(21474835.seconds)

  //
  // List of child actors that the interpreter contains
  //
  private var executeRequestTask: ActorRef = _
  private var completeCodeTask: ActorRef = _

  /**
   * Initializes all child actors performing tasks for the interpreter.
   */
  override def preStart = {
    executeRequestTask = interpreterTaskFactory.ExecuteRequestTask(
      context, InterpreterChildActorType.ExecuteRequestTask.toString)
    completeCodeTask = interpreterTaskFactory.CodeCompleteTask(
      context, InterpreterChildActorType.CodeCompleteTask.toString)
  }

  override def receive: Receive = {
    case (executeRequest: ExecuteRequest, parentMessage: KernelMessage,
      outputStream: OutputStream) =>
      val data = (executeRequest, parentMessage, outputStream)
      (executeRequestTask ? data) recover {
        case ex: Throwable =>
          logger.error(s"Could not execute code ${executeRequest.code} because "
            + s"of exception: ${ex.getMessage}")
          Right(ExecuteError(
            ex.getClass.getName,
            ex.getLocalizedMessage,
            ex.getStackTrace.map(_.toString).toList)
          )
      } pipeTo sender
    case (completeRequest: CompleteRequest) =>
      logger.debug(s"InterpreterActor requesting code completion for code " +
        s"${completeRequest.code}")
      (completeCodeTask ? completeRequest) recover {
        case ex: Throwable =>
          logger.error(s"Could not complete code ${completeRequest.code}: " +
            s"${ex.getMessage}")
          Right(ExecuteError(
            ex.getClass.getName,
            ex.getLocalizedMessage,
            ex.getStackTrace.map(_.toString).toList)
          )
      } pipeTo sender
  }
}
