/*
 * Copyright 2014 IBM Corp.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.ibm.spark.kernel.protocol.v5.handler

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.ibm.spark.kernel.protocol.v5
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.comm._
import com.ibm.spark.kernel.protocol.v5.content.{CommMsg, ClearOutput, CommOpen}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}

import scala.concurrent.duration._

class CommMsgHandlerSpec extends TestKit(
  ActorSystem("CommMsgHandlerSpec")
) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar
  with BeforeAndAfter
{
  private val TestCommId = UUID.randomUUID().toString
  private val TestTargetName = "some test target"

  private var kmBuilder: KMBuilder = _
  private var mockCommStorage: CommStorage = _
  private var mockCommCallbacks: CommCallbacks = _
  private var mockActorLoader: ActorLoader = _
  private var commMsgHandler: ActorRef = _
  private var kernelMessageRelayProbe: TestProbe = _
  private var statusDispatchProbe: TestProbe = _

  before {
    kmBuilder = KMBuilder()
    mockCommCallbacks = mock[CommCallbacks]
    mockCommStorage = mock[CommStorage]
    doReturn(mockCommCallbacks).when(mockCommStorage)(TestCommId)

    mockActorLoader = mock[ActorLoader]

    commMsgHandler = system.actorOf(Props(
      classOf[CommMsgHandler], mockActorLoader, mockCommStorage
    ))

    // Used to intercept responses
    kernelMessageRelayProbe = TestProbe()
    when(mockActorLoader.load(SystemActorType.KernelMessageRelay))
      .thenReturn(system.actorSelection(kernelMessageRelayProbe.ref.path.toString))

    // Used to intercept busy/idle messages
    statusDispatchProbe = new TestProbe(system)
    when(mockActorLoader.load(SystemActorType.StatusDispatch))
      .thenReturn(system.actorSelection(statusDispatchProbe.ref.path.toString))
  }

  describe("CommMsgHandler") {
    describe("#process") {
      it("should execute msg callbacks if the id is registered") {
        // Mark our id as registered
        doReturn(true).when(mockCommStorage).contains(TestCommId)

        // Send a comm_open message with the test target
        commMsgHandler ! kmBuilder
          .withHeader(v5.MessageType.CommMsg)
          .withContentString(CommMsg(TestCommId, v5.Data()))
          .build

        // Should receive a busy and an idle message
        statusDispatchProbe.receiveN(2, 200.milliseconds)

        // Verify that the msg callbacks were triggered along the way
        verify(mockCommCallbacks).executeMsgCallbacks(
          any[CommWriter], any[v5.UUID], any[v5.Data])
      }

      it("should not execute msg callbacks if the id is not registered") {
        // Mark our target as not registered
        doReturn(false).when(mockCommStorage).contains(TestCommId)

        // Send a comm_msg message with the test id
        commMsgHandler ! kmBuilder
          .withHeader(v5.MessageType.CommMsg)
          .withContentString(CommMsg(TestCommId, v5.Data()))
          .build

        // Should receive a busy and an idle message
        statusDispatchProbe.receiveN(2, 200.milliseconds)

        // Verify that the msg callbacks were NOT triggered along the way
        verify(mockCommCallbacks, never()).executeMsgCallbacks(
          any[CommWriter], any[v5.UUID], any[v5.Data])
      }

      it("should do nothing if there is a parsing error") {
        // Send a comm_open message with an invalid content string
        commMsgHandler ! kmBuilder
          .withHeader(v5.MessageType.CommMsg)
          .withContentString(ClearOutput(_wait = true))
          .build

        // TODO: Is there a better way to test for this without an upper time
        //       limit? Is there a different logical approach?
        kernelMessageRelayProbe.expectNoMsg(200.milliseconds)
      }
    }
  }
}
