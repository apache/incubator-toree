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

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.apache.toree.interpreter.{ExecuteAborted, ExecuteError}
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content._
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.kernel.protocol.v5.magic.{MagicParser, PostProcessor}
import com.typesafe.config.ConfigFactory
import org.apache.toree.plugins.PluginManager
import org.apache.toree.plugins.dependencies.DependencyManager
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}

object ExecuteRequestRelaySpec {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class ExecuteRequestRelaySpec extends TestKit(
  ActorSystem(
    "ExecuteRequestRelayActorSystem",
    ConfigFactory.parseString(ExecuteRequestRelaySpec.config)
  )
) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar
  with BeforeAndAfter
{
  var mockActorLoader: ActorLoader      = _
  var interpreterActorProbe: TestProbe  = _

  before {
    mockActorLoader = mock[ActorLoader]
    interpreterActorProbe = new TestProbe(system)
    val mockInterpreterActorSelection =
      system.actorSelection(interpreterActorProbe.ref.path.toString)
    doReturn(mockInterpreterActorSelection).when(mockActorLoader)
      .load(SystemActorType.Interpreter)
  }

  describe("ExecuteRequestRelay") {
    describe("#receive(KernelMessage)") {
      it("should handle an abort returned by the InterpreterActor") {
        val executeRequest =
          ExecuteRequest("%myMagic", false, true, UserExpressions(), true)

        val mockPostProcessor = mock[PostProcessor]
        val mockPluginManager = mock[PluginManager]
        val mockDependencyManager = mock[DependencyManager]
        doReturn(mockDependencyManager).when(mockPluginManager).dependencyManager

        val mockMagicParser = mock[MagicParser]
        doReturn(Left(executeRequest.code))
          .when(mockMagicParser).parse(executeRequest.code)

        val executeRequestRelay = system.actorOf(Props(
          classOf[ExecuteRequestRelay], mockActorLoader,
          mockPluginManager, mockMagicParser, mockPostProcessor
        ))

        // Send the message to the ExecuteRequestRelay
        executeRequestRelay !
          ((executeRequest, mock[KernelMessage], mock[OutputStream]))

        // Expected does not actually match real return of magic, which
        // is a tuple of ExecuteReply and ExecuteResult
        val expected = new ExecuteAborted()
        interpreterActorProbe.expectMsgClass(
          classOf[(ExecuteRequest, KernelMessage, OutputStream)]
        )

        // Reply with an error
        interpreterActorProbe.reply(Right(expected))

        expectMsg(
          (ExecuteReplyAbort(1), ExecuteResult(1, Data(), Metadata()))
        )
      }

      it("should handle an error returned by the InterpreterActor") {
        val executeRequest =
          ExecuteRequest("%myMagic", false, true, UserExpressions(), true)

        val mockPostProcessor = mock[PostProcessor]
        val mockPluginManager = mock[PluginManager]
        val mockDependencyManager = mock[DependencyManager]
        doReturn(mockDependencyManager).when(mockPluginManager).dependencyManager

        val mockMagicParser = mock[MagicParser]
        doReturn(Left(executeRequest.code))
          .when(mockMagicParser).parse(executeRequest.code)

        val executeRequestRelay = system.actorOf(Props(
          classOf[ExecuteRequestRelay], mockActorLoader,
          mockPluginManager, mockMagicParser, mockPostProcessor
        ))

        // Send the message to the ExecuteRequestRelay
        executeRequestRelay !
          ((executeRequest, mock[KernelMessage], mock[OutputStream]))

        // Expected does not actually match real return of magic, which
        // is a tuple of ExecuteReply and ExecuteResult
        val expected = ExecuteError("NAME", "MESSAGE", List())
        interpreterActorProbe.expectMsgClass(
          classOf[(ExecuteRequest, KernelMessage, OutputStream)]
        )

        // Reply with an error
        interpreterActorProbe.reply(Right(expected))

        expectMsg((
          ExecuteReplyError(1, Some(expected.name), Some(expected.value),
            Some(expected.stackTrace.map(_.toString).toList)),
          ExecuteResult(1, Data("text/plain" -> expected.toString), Metadata())
        ))
      }

      it("should return an (ExecuteReply, ExecuteResult) on interpreter " +
         "success") {
        val expected = "SOME OTHER VALUE"
        val executeRequest =
          ExecuteRequest("notAMagic", false, true, UserExpressions(), true)

        val mockPostProcessor = mock[PostProcessor]
        doReturn(Data(MIMEType.PlainText -> expected))
          .when(mockPostProcessor).process(expected)
        val mockPluginManager = mock[PluginManager]
        val mockDependencyManager = mock[DependencyManager]
        doReturn(mockDependencyManager).when(mockPluginManager).dependencyManager

        val mockMagicParser = mock[MagicParser]
        doReturn(Left(executeRequest.code))
          .when(mockMagicParser).parse(executeRequest.code)

        val executeRequestRelay = system.actorOf(Props(
          classOf[ExecuteRequestRelay], mockActorLoader,
          mockPluginManager, mockMagicParser, mockPostProcessor
        ))

        // Send the message to the ExecuteRequestRelay
        executeRequestRelay !
          ((executeRequest, mock[KernelMessage], mock[OutputStream]))

        // Expected does not actually match real return of interpreter, which
        // is a tuple of ExecuteReply and ExecuteResult
        interpreterActorProbe.expectMsgClass(
          classOf[(ExecuteRequest, KernelMessage, OutputStream)]
        )

        // Reply with a successful interpret
        interpreterActorProbe.reply(Left(expected))

        expectMsg((
          ExecuteReplyOk(1, Some(Payloads()), Some(UserExpressions())),
          ExecuteResult(1, Data(MIMEType.PlainText -> expected), Metadata())
        ))
      }
    }
  }
}
