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

package org.apache.toree.kernel.api

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.apache.toree.kernel.protocol.v5
import org.apache.toree.kernel.protocol.v5.KernelMessage
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpecLike, BeforeAndAfter, Matchers}
import play.api.libs.json.Json
import test.utils.MaxAkkaTestTimeout
import org.mockito.Mockito._

class StreamMethodsSpec extends TestKit(
  ActorSystem(
    "StreamMethodsSpec",
    None,
    Some(org.apache.toree.Main.getClass.getClassLoader)
  )
) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar
  with BeforeAndAfter
{

  private var kernelMessageRelayProbe: TestProbe = _
  private var mockParentHeader: v5.ParentHeader = _
  private var mockActorLoader: v5.kernel.ActorLoader = _
  private var mockKernelMessage: v5.KernelMessage = _
  private var streamMethods: StreamMethods = _

  before {
    kernelMessageRelayProbe = TestProbe()

    mockParentHeader = mock[v5.ParentHeader]

    mockActorLoader = mock[v5.kernel.ActorLoader]
    doReturn(system.actorSelection(kernelMessageRelayProbe.ref.path))
      .when(mockActorLoader).load(v5.SystemActorType.KernelMessageRelay)

    mockKernelMessage = mock[v5.KernelMessage]
    doReturn(mockParentHeader).when(mockKernelMessage).header

    streamMethods = new StreamMethods(mockActorLoader, mockKernelMessage)
  }

  describe("StreamMethods") {
    describe("#()") {
      it("should put the header of the given message as the parent header") {
        val expected = mockKernelMessage.header
        val actual = streamMethods.kmBuilder.build.parentHeader

        actual should be (expected)
      }
    }

    describe("#sendAll") {
      it("should send a message containing all of the given text") {
        val expected = "some text"

        streamMethods.sendAll(expected)

        val outgoingMessage = kernelMessageRelayProbe.receiveOne(MaxAkkaTestTimeout)
        val kernelMessage = outgoingMessage.asInstanceOf[KernelMessage]

        val actual = Json.parse(kernelMessage.contentString)
          .as[v5.content.StreamContent].text

        actual should be (expected)
      }
    }
  }

}
