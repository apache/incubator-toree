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
import java.util.concurrent.TimeUnit
import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.apache.toree.interpreter.{ExecuteAborted, ExecuteError}
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content._
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.kernel.protocol.v5.magic.MagicParser
import com.typesafe.config.ConfigFactory
import org.apache.toree.plugins.PluginManager
import org.apache.toree.plugins.dependencies.DependencyManager
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}
import test.utils.MaxAkkaTestTimeout
import scala.concurrent.duration.Duration

object ExecuteRequestRelaySpec {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class ExecuteRequestRelaySpec extends TestKit(
  ActorSystem(
    "ExecuteRequestRelayActorSystem",
    ConfigFactory.parseString(ExecuteRequestRelaySpec.config),
    org.apache.toree.Main.getClass.getClassLoader
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

        val mockPluginManager = mock[PluginManager]
        val mockDependencyManager = mock[DependencyManager]
        doReturn(mockDependencyManager).when(mockPluginManager).dependencyManager

        val mockMagicParser = mock[MagicParser]
        doReturn(Left(executeRequest.code))
          .when(mockMagicParser).parse(executeRequest.code)

        val executeRequestRelay = system.actorOf(Props(
          classOf[ExecuteRequestRelay], mockActorLoader,
          mockPluginManager, mockMagicParser
        ))

        // Send the message to the ExecuteRequestRelay
        executeRequestRelay !
          ((executeRequest, mock[KernelMessage], mock[OutputStream]))

        // Expected does not actually match real return of magic, which
        // is a tuple of ExecuteReply and ExecuteResult
        val expected = new ExecuteAborted()
        interpreterActorProbe.expectMsgClass(max = Duration(5, TimeUnit.SECONDS),
          classOf[(ExecuteRequest, KernelMessage, OutputStream)]
        )

        // Reply with an error
        interpreterActorProbe.reply(Right(expected))

        expectMsg(
          MaxAkkaTestTimeout,
          (ExecuteReplyAbort(1), ExecuteResult(1, Data(), Metadata()))
        )
      }

      it("should handle an error returned by the InterpreterActor") {
        val executeRequest =
          ExecuteRequest("%myMagic", false, true, UserExpressions(), true)

        val mockPluginManager = mock[PluginManager]
        val mockDependencyManager = mock[DependencyManager]
        doReturn(mockDependencyManager).when(mockPluginManager).dependencyManager

        val mockMagicParser = mock[MagicParser]
        doReturn(Left(executeRequest.code))
          .when(mockMagicParser).parse(executeRequest.code)

        val executeRequestRelay = system.actorOf(Props(
          classOf[ExecuteRequestRelay], mockActorLoader,
          mockPluginManager, mockMagicParser
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

        expectMsg(
          MaxAkkaTestTimeout,
          (
            ExecuteReplyError(1, Some(expected.name), Some(expected.value),
            Some(expected.stackTrace.map(_.toString).toList)),
            ExecuteResult(1, Data("text/plain" -> expected.toString), Metadata())
          )
        )
      }

      it("should return an (ExecuteReply, ExecuteResult) on interpreter " +
         "success") {
        val expected = Map(MIMEType.PlainText -> "SOME OTHER VALUE")
        val executeRequest =
          ExecuteRequest("notAMagic", false, true, UserExpressions(), true)

        val mockPluginManager = mock[PluginManager]
        val mockDependencyManager = mock[DependencyManager]
        doReturn(mockDependencyManager).when(mockPluginManager).dependencyManager

        val mockMagicParser = mock[MagicParser]
        doReturn(Left(executeRequest.code))
          .when(mockMagicParser).parse(executeRequest.code)

        val executeRequestRelay = system.actorOf(Props(
          classOf[ExecuteRequestRelay], mockActorLoader,
          mockPluginManager, mockMagicParser
        ))

        // Send the message to the ExecuteRequestRelay
        executeRequestRelay !
          ((executeRequest, mock[KernelMessage], mock[OutputStream]))

        // Expected does not actually match real return of interpreter, which
        // is a tuple of ExecuteReply and ExecuteResult
        interpreterActorProbe.expectMsgClass(
          MaxAkkaTestTimeout,
          classOf[(ExecuteRequest, KernelMessage, OutputStream)]
        )

        // Reply with a successful interpret
        interpreterActorProbe.reply(Left(expected))

        expectMsg(
          MaxAkkaTestTimeout,
          (
            ExecuteReplyOk(1, Some(Payloads()), Some(UserExpressions())),
            ExecuteResult(1, expected, Metadata())
          )
        )
      }
    }
  }
}
