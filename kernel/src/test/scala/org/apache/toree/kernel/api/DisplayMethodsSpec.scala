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
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}
import play.api.libs.json.Json
import test.utils.MaxAkkaTestTimeout

class DisplayMethodsSpec extends TestKit(
  ActorSystem(
    "DisplayMethodsSpec",
    None,
    Some(org.apache.toree.Main.getClass.getClassLoader)
  )) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar
  with BeforeAndAfter
{
  

  private var kernelMessageRelayProbe: TestProbe = _
  private var mockParentHeader: v5.ParentHeader = _
  private var mockActorLoader: v5.kernel.ActorLoader = _
  private var mockKernelMessage: v5.KernelMessage = _
  private var displayMethods: DisplayMethods = _

  before {
    kernelMessageRelayProbe = TestProbe()

    mockParentHeader = mock[v5.ParentHeader]

    mockActorLoader = mock[v5.kernel.ActorLoader]
    doReturn(system.actorSelection(kernelMessageRelayProbe.ref.path))
      .when(mockActorLoader).load(v5.SystemActorType.KernelMessageRelay)

    mockKernelMessage = mock[v5.KernelMessage]
    doReturn(mockParentHeader).when(mockKernelMessage).header

    displayMethods = new DisplayMethods(mockActorLoader, mockKernelMessage, v5.KMBuilder())
  }

  describe("DisplayMethods") {
    describe("#()") {
      it("should put the header of the given message as the parent header") {
        val expected = mockKernelMessage.header
        val actual = displayMethods.kmBuilder.build.parentHeader

        actual should be (expected)
      }
    }

    describe("#content") {
      it("should send a message containing all the contents and mimetype") {
        val expectedContent = "some text"
        val expectedMimeType = "some mime type"

        displayMethods.content(expectedMimeType, expectedContent)

        val outgoingMessage = kernelMessageRelayProbe.receiveOne(MaxAkkaTestTimeout)
        val kernelMessage = outgoingMessage.asInstanceOf[KernelMessage]

        val actualDisplayData = Json.parse(kernelMessage.contentString)
          .as[v5.content.DisplayData].data

        actualDisplayData should contain (expectedMimeType -> expectedContent)
      }
    }

    describe("#html") {
      it("should send a message containing the contents and the html mimetype") {
        val expectedContent = "some text"

        displayMethods.html(expectedContent)

        val outgoingMessage = kernelMessageRelayProbe.receiveOne(MaxAkkaTestTimeout)
        val kernelMessage = outgoingMessage.asInstanceOf[KernelMessage]

        val actualDisplayData = Json.parse(kernelMessage.contentString)
          .as[v5.content.DisplayData].data

        actualDisplayData should contain ("text/html" -> expectedContent)
      }
    }

    describe("#javascript") {
      it("should send a message containing the contents and the javascript mimetype") {
        val expectedContent = "some text"

        displayMethods.javascript(expectedContent)

        val outgoingMessage = kernelMessageRelayProbe.receiveOne(MaxAkkaTestTimeout)
        val kernelMessage = outgoingMessage.asInstanceOf[KernelMessage]

        val actualDisplayData = Json.parse(kernelMessage.contentString)
          .as[v5.content.DisplayData].data

        actualDisplayData should contain ("application/javascript" -> expectedContent)
      }
    }

    describe("#clear") {
      it("should send a clear-output message with wait false when no args passed") {

        displayMethods.clear()

        val outgoingMessage = kernelMessageRelayProbe.receiveOne(MaxAkkaTestTimeout)
        val kernelMessage = outgoingMessage.asInstanceOf[KernelMessage]

        val actualClearOutput = Json.parse(kernelMessage.contentString)
          .as[v5.content.ClearOutput]

        actualClearOutput._wait should be (false)
      }

      it("should send a clear-output message with wait false when false passed") {

        displayMethods.clear(false)

        val outgoingMessage = kernelMessageRelayProbe.receiveOne(MaxAkkaTestTimeout)
        val kernelMessage = outgoingMessage.asInstanceOf[KernelMessage]

        val actualClearOutput = Json.parse(kernelMessage.contentString)
          .as[v5.content.ClearOutput]

        actualClearOutput._wait should be (false)
      }

      it("should send a clear-output message with wait true when true passed") {

        displayMethods.clear(true)

        val outgoingMessage = kernelMessageRelayProbe.receiveOne(MaxAkkaTestTimeout)
        val kernelMessage = outgoingMessage.asInstanceOf[KernelMessage]

        val actualClearOutput = Json.parse(kernelMessage.contentString)
          .as[v5.content.ClearOutput]

        actualClearOutput._wait should be (true)
      }
    }
  }

}
