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

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.apache.toree.interpreter._
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content._
import org.mockito.Matchers.{anyBoolean, anyString, anyObject}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}
import test.utils.MaxAkkaTestTimeout

object ExecuteRequestTaskActorSpec {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class ExecuteRequestTaskActorSpec extends TestKit(
  ActorSystem(
    "ExecuteRequestTaskActorSpec",
    ConfigFactory.parseString(ExecuteRequestTaskActorSpec.config),
    org.apache.toree.Main.getClass.getClassLoader
  )
) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar
{
  describe("ExecuteRequestTaskActor") {
    describe("#receive") {
      it("should return an ExecuteReplyOk if the interpreter returns success") {
        val mockInterpreter = mock[Interpreter]
        doReturn((Results.Success, Left(Map()))).when(mockInterpreter)
          .interpret(anyString(), anyBoolean(), anyObject())

        val executeRequestTask =
          system.actorOf(Props(
            classOf[ExecuteRequestTaskActor],
            mockInterpreter
          ))

        val executeRequest = (ExecuteRequest(
          "val x = 3", false, false,
          UserExpressions(), false
        ), mock[KernelMessage], mock[OutputStream])

        executeRequestTask ! executeRequest

        val result =
          receiveOne(MaxAkkaTestTimeout)
            .asInstanceOf[Either[ExecuteOutput, ExecuteError]]

        result.isLeft should be (true)
        result.left.get shouldBe an [ExecuteOutput]
      }

      it("should return an ExecuteReplyAbort if the interpreter returns aborted") {
        val mockInterpreter = mock[Interpreter]
        doReturn((Results.Aborted, Right(mock[ExecuteAborted]))).when(mockInterpreter)
          .interpret(anyString(), anyBoolean(), anyObject())

        val executeRequestTask =
          system.actorOf(Props(
            classOf[ExecuteRequestTaskActor],
            mockInterpreter
          ))

        val executeRequest = (ExecuteRequest(
          "val x = 3", false, false,
          UserExpressions(), false
        ), mock[KernelMessage], mock[OutputStream])

        executeRequestTask ! executeRequest

        val result =
          receiveOne(MaxAkkaTestTimeout)
            .asInstanceOf[Either[ExecuteOutput, ExecuteFailure]]

        result.isRight should be (true)
        result.right.get shouldBe an [ExecuteAborted]
      }

      it("should return an ExecuteReplyError if the interpreter returns error") {
        val mockInterpreter = mock[Interpreter]
        doReturn((Results.Error, Right(mock[ExecuteError]))).when(mockInterpreter)
          .interpret(anyString(), anyBoolean(), anyObject())

        val executeRequestTask =
          system.actorOf(Props(
            classOf[ExecuteRequestTaskActor],
            mockInterpreter
          ))

        val executeRequest = (ExecuteRequest(
          "val x = 3", false, false,
          UserExpressions(), false
        ), mock[KernelMessage], mock[OutputStream])

        executeRequestTask ! executeRequest

        val result =
          receiveOne(MaxAkkaTestTimeout)
            .asInstanceOf[Either[ExecuteOutput, ExecuteError]]

        result.isRight should be (true)
        result.right.get shouldBe an [ExecuteError]
      }

      it("should return ExecuteReplyError if interpreter returns incomplete") {
        val mockInterpreter = mock[Interpreter]
        doReturn((Results.Incomplete, Right(""))).when(mockInterpreter)
          .interpret(anyString(), anyBoolean(), anyObject())

        val executeRequestTask =
          system.actorOf(Props(
            classOf[ExecuteRequestTaskActor],
            mockInterpreter
          ))

        val executeRequest = (ExecuteRequest(
          "(1 + 2", false, false,
          UserExpressions(), false
        ), mock[KernelMessage], mock[OutputStream])

        executeRequestTask ! executeRequest

        val result =
          receiveOne(MaxAkkaTestTimeout)
            .asInstanceOf[Either[ExecuteOutput, ExecuteError]]

        result.isLeft should be (false)
        result.right.get shouldBe an [ExecuteError]
      }

      it("should return an ExecuteReplyOk when receiving empty code.") {
        val mockInterpreter = mock[Interpreter]
        doReturn((Results.Incomplete, Right(""))).when(mockInterpreter)
          .interpret(anyString(), anyBoolean(), anyObject())

        val executeRequestTask =
          system.actorOf(Props(
            classOf[ExecuteRequestTaskActor],
            mockInterpreter
          ))

        val executeRequest = (ExecuteRequest(
          "   ", false, false,
          UserExpressions(), false
        ), mock[KernelMessage], mock[OutputStream])

        executeRequestTask ! executeRequest

        val result =
          receiveOne(MaxAkkaTestTimeout)
            .asInstanceOf[Either[ExecuteOutput, ExecuteError]]

        result.isLeft should be (true)
        result.isRight should be (false)
      }
    }
  }
}
