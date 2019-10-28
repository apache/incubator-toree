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

import java.util.UUID

import akka.actor.{Props, ActorRef, ActorSystem}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import org.apache.toree.kernel.protocol.v5
import org.apache.toree.kernel.protocol.v5.content.{ClearOutput, CommClose}
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.kernel.protocol.v5.{KernelMessage, SystemActorType, KMBuilder}
import org.apache.toree.comm.{CommRegistrar, CommWriter, CommCallbacks, CommStorage}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}
import org.mockito.Mockito._
import org.mockito.Matchers._
import test.utils.MaxAkkaTestTimeout

class CommCloseHandlerSpec extends TestKit(
  ActorSystem(
    "CommCloseHandlerSpec",
    None,
    Some(org.apache.toree.Main.getClass.getClassLoader)
  )
) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar
  with BeforeAndAfter
{
  private val TestCommId = UUID.randomUUID().toString
  private var kmBuilder: KMBuilder = _
  private var spyCommStorage: CommStorage = _
  private var mockCommCallbacks: CommCallbacks = _
  private var mockCommRegistrar: CommRegistrar = _
  private var mockActorLoader: ActorLoader = _
  private var commCloseHandler: ActorRef = _
  private var kernelMessageRelayProbe: TestProbe = _
  private var statusDispatchProbe: TestProbe = _

  before {
    kmBuilder = KMBuilder()
    mockCommCallbacks = mock[CommCallbacks]
    spyCommStorage = spy(new CommStorage())
    mockCommRegistrar = mock[CommRegistrar]

    mockActorLoader = mock[ActorLoader]

    commCloseHandler = system.actorOf(Props(
      classOf[CommCloseHandler],
      mockActorLoader, mockCommRegistrar, spyCommStorage
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

  describe("CommCloseHandler") {
    describe("#process") {
      it("should execute close callbacks if the id is registered") {
        // Mark our id as registered
        doReturn(Some(mockCommCallbacks)).when(spyCommStorage)
          .getCommIdCallbacks(TestCommId)

        // Send a comm_open message with the test target
        commCloseHandler ! kmBuilder
          .withHeader(CommClose.toTypeString)
          .withContentString(CommClose(TestCommId, v5.MsgData.Empty))
          .build

        // Should receive a busy and an idle message
        statusDispatchProbe.receiveN(2, MaxAkkaTestTimeout)

        // Verify that the msg callbacks were triggered along the way
        verify(mockCommCallbacks).executeCloseCallbacks(
          any[CommWriter], any[v5.UUID], any[v5.MsgData])
      }

      it("should not execute close callbacks if the id is not registered") {
        // Mark our target as not registered
        doReturn(None).when(spyCommStorage).getCommIdCallbacks(TestCommId)

        // Send a comm_msg message with the test id
        commCloseHandler ! kmBuilder
          .withHeader(CommClose.toTypeString)
          .withContentString(CommClose(TestCommId, v5.MsgData.Empty))
          .build

        // Should receive a busy and an idle message
        statusDispatchProbe.receiveN(2, MaxAkkaTestTimeout)

        // Verify that the msg callbacks were NOT triggered along the way
        verify(mockCommCallbacks, never()).executeCloseCallbacks(
          any[CommWriter], any[v5.UUID], any[v5.MsgData])
      }

      it("should do nothing if there is a parsing error") {
        // Send a comm_open message with an invalid content string
        commCloseHandler ! kmBuilder
          .withHeader(CommClose.toTypeString)
          .withContentString(ClearOutput(_wait = true))
          .build

        // TODO: Is there a better way to test for this without an upper time
        //       limit? Is there a different logical approach?
        kernelMessageRelayProbe.expectNoMsg(MaxAkkaTestTimeout)
      }

      it("should include the parent's header in the parent header of " +
        "outgoing messages"){

        // Register a callback that sends a message using the comm writer
        val closeCallback: CommCallbacks.CloseCallback =
          new CommCallbacks.CloseCallback() {
            def apply(v1: CommWriter, v2: v5.UUID, v4: v5.MsgData) =
              v1.writeMsg(v5.MsgData.Empty)
          }
        val callbacks = (new CommCallbacks).addCloseCallback(closeCallback)
        doReturn(Some(callbacks)).when(spyCommStorage)
          .getCommIdCallbacks(TestCommId)

        // Send a comm close message
        val msg = kmBuilder
          .withHeader(CommClose.toTypeString)
          .withContentString(CommClose(TestCommId, v5.MsgData.Empty))
          .build
        commCloseHandler ! msg

        // Verify that the message sent by the handler has the desired property
        kernelMessageRelayProbe.fishForMessage(MaxAkkaTestTimeout) {
          case KernelMessage(_, _, _, parentHeader, _, _) =>
            parentHeader == msg.header
        }
      }
    }
  }
}
