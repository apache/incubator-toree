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

import akka.actor._
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import org.apache.toree.Main
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content.CompleteRequest
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.kernel.protocol.v5Test._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpecLike, BeforeAndAfter, Matchers}
import org.mockito.Mockito._
import test.utils.MaxAkkaTestTimeout

class CodeCompleteHandlerSpec extends TestKit(
  ActorSystem("CodeCompleteHandlerSpec", None, Some(Main.getClass.getClassLoader))
) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar
  with BeforeAndAfter {

  var actorLoader: ActorLoader = _
  var handlerActor: ActorRef = _
  var kernelMessageRelayProbe: TestProbe = _
  var interpreterProbe: TestProbe = _
  var statusDispatchProbe: TestProbe = _

  before {
    actorLoader = mock[ActorLoader]

    handlerActor = system.actorOf(Props(classOf[CodeCompleteHandler], actorLoader))

    kernelMessageRelayProbe = TestProbe()
    when(actorLoader.load(SystemActorType.KernelMessageRelay))
      .thenReturn(system.actorSelection(kernelMessageRelayProbe.ref.path.toString))

    interpreterProbe = new TestProbe(system)
    when(actorLoader.load(SystemActorType.Interpreter))
      .thenReturn(system.actorSelection(interpreterProbe.ref.path.toString))

    statusDispatchProbe = new TestProbe(system)
    when(actorLoader.load(SystemActorType.StatusDispatch))
      .thenReturn(system.actorSelection(statusDispatchProbe.ref.path.toString))
  }

  def replyToHandlerWithOkAndResult() = {
    val expectedClass = classOf[CompleteRequest]
    interpreterProbe.expectMsgClass(expectedClass)
    interpreterProbe.reply((0, List[String]()))
  }

  def replyToHandlerWithOkAndBadResult() = {
    val expectedClass = classOf[CompleteRequest]
    interpreterProbe.expectMsgClass(expectedClass)
    interpreterProbe.reply("hello")
  }

  describe("CodeCompleteHandler (ActorLoader)") {
    it("should send a CompleteRequest") {
      handlerActor ! MockCompleteRequestKernelMessage
      replyToHandlerWithOkAndResult()
      kernelMessageRelayProbe.fishForMessage(MaxAkkaTestTimeout) {
        case KernelMessage(_, _, header, _, _, _) =>
          header.msg_type == MessageType.Outgoing.CompleteReply.toString
      }
    }

    it("should throw an error for bad JSON") {
      handlerActor ! MockKernelMessageWithBadJSON
      var result = false
      try {
        replyToHandlerWithOkAndResult()
      }
      catch {
        case t: Throwable => result = true
      }
      result should be (true)
    }

    it("should throw an error for bad code completion") {
      handlerActor ! MockCompleteRequestKernelMessage
      try {
        replyToHandlerWithOkAndBadResult()
      }
      catch {
        case error: Exception => error.getMessage should be ("Parse error in CodeCompleteHandler")
      }
    }

    it("should send an idle message") {
      handlerActor ! MockCompleteRequestKernelMessage
      replyToHandlerWithOkAndResult()
      statusDispatchProbe.fishForMessage(MaxAkkaTestTimeout) {
        case Tuple2(status, _) =>
          status == KernelStatusType.Idle
      }
    }
  }
}
