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
import java.util.concurrent.ConcurrentHashMap
import akka.actor.{Props, ActorRef, ActorSystem}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import org.apache.toree.Main
import org.apache.toree.kernel.protocol.v5.content.InputReply
import org.apache.toree.kernel.protocol.v5.{HeaderBuilder, MessageType, KMBuilder, SystemActorType}
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.scalatest.concurrent.Eventually
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.time.{Milliseconds, Span}
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}
import test.utils.MaxAkkaTestTimeout
import org.mockito.Mockito._

import collection.JavaConverters._

class InputRequestReplyHandlerSpec
  extends TestKit(ActorSystem("InputRequestReplyHandlerSystem", None, Some(Main.getClass.getClassLoader)))
  with ImplicitSender with FunSpecLike with Matchers with MockitoSugar
  with BeforeAndAfter with Eventually
{
  implicit override val patienceConfig = PatienceConfig(
    timeout = scaled(Span(200, Milliseconds)),
    interval = scaled(Span(5, Milliseconds))
  )

  private var fakeSender: TestProbe = _
  private var kernelMessageRelayProbe: TestProbe = _
  private var mockActorLoader: ActorLoader = _
  private var responseMap: collection.mutable.Map[String, ActorRef] = _

  private var inputRequestReplyHandler: ActorRef = _

  before {
    fakeSender = TestProbe()

    kernelMessageRelayProbe = TestProbe()
    mockActorLoader = mock[ActorLoader]
    doReturn(system.actorSelection(kernelMessageRelayProbe.ref.path.toString))
      .when(mockActorLoader).load(SystemActorType.KernelMessageRelay)

    responseMap = new ConcurrentHashMap[String, ActorRef]().asScala

    inputRequestReplyHandler = system.actorOf(Props(
      classOf[InputRequestReplyHandler], mockActorLoader, responseMap
    ))
  }

  describe("InputRequestReplyHandler") {
    describe("#receive") {
      it("should store the sender under the session if input_request") {
        val session = UUID.randomUUID().toString
        val inputRequestMessage = KMBuilder()
          .withParentHeader(HeaderBuilder.empty.copy(session = session))
          .withHeader(MessageType.Outgoing.InputRequest)
          .build

        fakeSender.send(inputRequestReplyHandler, inputRequestMessage)

        eventually {
          responseMap(session) should be (fakeSender.ref)
        }
      }

      it("should forward message to relay if input_request") {
        val inputRequestMessage = KMBuilder()
          .withHeader(MessageType.Outgoing.InputRequest)
          .build

        fakeSender.send(inputRequestReplyHandler, inputRequestMessage)

        kernelMessageRelayProbe.expectMsg(MaxAkkaTestTimeout, inputRequestMessage)
      }

      it("should send the received message value to the stored sender with " +
        "the same session if input_reply") {
        val expected = "some value"
        val session = UUID.randomUUID().toString

        // Build the message to "get back from the world"
        val inputReplyMessage = KMBuilder()
          .withHeader(HeaderBuilder.empty.copy(
            msg_type = MessageType.Incoming.InputReply.toString,
            session = session
          ))
          .withContentString(InputReply(expected))
          .build

        // Add our fake sender actor to the receiving end of the message
        responseMap(session) = fakeSender.ref

        fakeSender.send(inputRequestReplyHandler, inputReplyMessage)

        // Sender should receive a response
        fakeSender.expectMsg(MaxAkkaTestTimeout, expected)
      }

      it("should do nothing if the session is not found for input_reply") {
        val expected = "some value"
        val session = UUID.randomUUID().toString

        // Build the message to "get back from the world"
        val inputReplyMessage = KMBuilder()
          .withHeader(HeaderBuilder.empty.copy(
          msg_type = MessageType.Incoming.InputReply.toString,
          session = session
        ))
          .withContentString(InputReply(expected))
          .build

        fakeSender.send(inputRequestReplyHandler, inputReplyMessage)

        // Sender should not receive a response
        fakeSender.expectNoMessage(MaxAkkaTestTimeout)
      }
    }
  }
}
