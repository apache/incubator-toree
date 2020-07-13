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

package org.apache.toree.kernel.protocol.v5.stream

import akka.actor.{ActorRef, Actor, ActorSystem}
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import org.apache.toree.Main
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content.InputRequest
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.mockito.Mockito._
import org.scalatest._
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import test.utils._

class KernelInputStreamSpec
  extends TestKit(ActorSystem("KernelInputStreamActorSystem", None, Some(Main.getClass.getClassLoader)))
  with FunSpecLike with Matchers with GivenWhenThen with BeforeAndAfter
  with MockitoSugar
{

  private var mockActorLoader: ActorLoader = _
  private var mockKMBuilder: KMBuilder = _
  private var kernelInputOutputHandlerProbe: TestProbe = _
  private var kernelInputStream: KernelInputStream = _
  private var fakeInputOutputHandlerActor: ActorRef = _

  private val TestReplyString = "some reply"

  before {
    mockActorLoader = mock[ActorLoader]
    mockKMBuilder = KMBuilder() // No need to really mock this

    kernelInputStream = new KernelInputStream(mockActorLoader, mockKMBuilder)

    kernelInputOutputHandlerProbe = TestProbe()
    fakeInputOutputHandlerActor = TestActorRef(new Actor {
      override def receive: Receive = {
        // Handle case for getting an input_request
        case kernelMessage: KernelMessage =>
          val messageType = kernelMessage.header.msg_type
          kernelInputOutputHandlerProbe.ref ! kernelMessage
          if (messageType == MessageType.Outgoing.InputRequest.toString)
            sender ! TestReplyString
      }
    })

    // Add the actor that routes to our test probe and responds with a fake
    // set of data
    doReturn(system.actorSelection(fakeInputOutputHandlerActor.path.toString))
      .when(mockActorLoader).load(MessageType.Incoming.InputReply)
    // Allow time for the actors to start. This avoids read() hanging forever
    // when running tests in gradle.
    Thread.sleep(100)
  }

  describe("KernelInputStream") {
    describe("#available") {
      it("should be zero when no input has been read") {
        kernelInputStream.available() should be (0)
      }

      it("should match the bytes remaining internally") {
        kernelInputStream.read()

        kernelInputStream.available() should be (TestReplyString.length - 1)
      }
    }

    describe("#read") {
      it("should send a request for more data if the buffer is empty") {
        // Fresh input stream has nothing in its buffer
        kernelInputStream.read()

        // Verify that a message was sent out requesting data
        kernelInputOutputHandlerProbe.expectMsgPF(MaxAkkaTestTimeout) {
          case KernelMessage(_, _, header, _, _, _)
            if header.msg_type == MessageType.Outgoing.InputRequest.toString =>
              true
        }
      }

      it("should use the provided prompt in its requests") {
        val expected = KernelInputStream.DefaultPrompt

        // Fresh input stream has nothing in its buffer
        kernelInputStream.read()

        // Verify that a message was sent out requesting data with the
        // specific prompt
        kernelInputOutputHandlerProbe.expectMsgPF(MaxAkkaTestTimeout) {
          case KernelMessage(_, _, header, _, _, contentString)
            if header.msg_type == MessageType.Outgoing.InputRequest.toString =>
            Json.parse(contentString).as[InputRequest].prompt should be (expected)
        }
      }

      it("should use the provided password flag in its requests") {
        val expected = KernelInputStream.DefaultPassword

        // Fresh input stream has nothing in its buffer
        kernelInputStream.read()

        // Verify that a message was sent out requesting data with the
        // specific prompt
        kernelInputOutputHandlerProbe.expectMsgPF(MaxAkkaTestTimeout) {
          case KernelMessage(_, _, header, _, _, contentString)
            if header.msg_type == MessageType.Outgoing.InputRequest.toString =>
            Json.parse(contentString).as[InputRequest].password should be (expected)
        }
      }

      it("should return the next byte from the current buffer") {
        kernelInputStream.read() should be (TestReplyString.head)
      }

      it("should not send a request for more data if data is in the buffer") {
        // Run read for length of message (avoiding sending out a second
        // request)
        val readLength = TestReplyString.length

        for (i <- 1 to readLength)
          kernelInputStream.read() should be (TestReplyString.charAt(i - 1))

        kernelInputOutputHandlerProbe.expectMsgClass(MaxAkkaTestTimeout, classOf[KernelMessage])
        kernelInputOutputHandlerProbe.expectNoMessage(MaxAkkaTestTimeout)
      }
    }
  }
}
