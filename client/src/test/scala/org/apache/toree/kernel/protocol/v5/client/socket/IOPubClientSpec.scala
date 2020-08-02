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

package org.apache.toree.kernel.protocol.v5.client.socket

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import org.apache.toree.comm.{CommCallbacks, CommRegistrar, CommStorage, CommWriter}
import org.apache.toree.communication.ZMQMessage
import org.apache.toree.kernel.protocol.v5
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.client.Utilities._
import org.apache.toree.kernel.protocol.v5.client.execution.{DeferredExecution, DeferredExecutionManager}
import org.apache.toree.kernel.protocol.v5.client.{ActorLoader, Utilities}
import org.apache.toree.kernel.protocol.v5.content.{CommClose, CommMsg, CommOpen, StreamContent}
import com.typesafe.config.ConfigFactory
import org.mockito.Matchers.{eq => mockEq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.time.{Milliseconds, Span}
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.util.Failure

object IOPubClientSpec {
  val config ="""
    akka {
      loglevel = "WARNING"
    }"""
}

class IOPubClientSpec extends TestKit(ActorSystem(
  "IOPubClientSpecSystem", ConfigFactory.parseString(IOPubClientSpec.config)
)) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar
  with ScalaFutures with BeforeAndAfter with Eventually
{
  private val TestTimeout = Timeout(10.seconds)
  implicit override val patienceConfig = PatienceConfig(
    timeout = scaled(Span(200, Milliseconds)),
    interval = scaled(Span(5, Milliseconds))
  )
  private val SignatureEnabled = true

  private var clientSocketProbe: TestProbe = _
  private var mockClientSocketFactory: SocketFactory = _
  private var mockActorLoader: ActorLoader = _
  private var mockCommRegistrar: CommRegistrar = _
  private var spyCommStorage: CommStorage = _
  private var mockCommCallbacks: CommCallbacks = _
  private var ioPubClient: ActorRef = _

  private var kmBuilder: KMBuilder = _

  private val id = UUID.randomUUID().toString
  private val TestTargetName = "some target"
  private val TestCommId = UUID.randomUUID().toString

  before {
    kmBuilder = KMBuilder()
    mockCommCallbacks = mock[CommCallbacks]
    mockCommRegistrar = mock[CommRegistrar]

    spyCommStorage = spy(new CommStorage())

    clientSocketProbe = TestProbe()
    mockActorLoader = mock[ActorLoader]
    mockClientSocketFactory = mock[SocketFactory]

    //  Stub the return value for the socket factory
    when(mockClientSocketFactory.IOPubClient(anyObject(), any[ActorRef]))
      .thenReturn(clientSocketProbe.ref)

    //  Construct the object we will test against
    ioPubClient = system.actorOf(Props(
      classOf[IOPubClient], mockClientSocketFactory, mockActorLoader,
      SignatureEnabled, mockCommRegistrar, spyCommStorage
    ))
  }

  describe("IOPubClient") {
    describe("#receive") {
      it("should execute all Comm open callbacks on comm_open message") {
        val message: ZMQMessage = kmBuilder
          .withHeader(CommOpen.toTypeString)
          .withContentString(CommOpen(TestCommId, TestTargetName, v5.MsgData.Empty))
          .build

        // Mark as target being provided
        doReturn(Some(mockCommCallbacks)).when(spyCommStorage)
          .getTargetCallbacks(anyString())

        // Simulate receiving a message from the kernel
        ioPubClient ! message

        // Check to see if "eventually" the callback is triggered
        eventually {
          verify(mockCommCallbacks).executeOpenCallbacks(
            any[CommWriter], mockEq(TestCommId),
            mockEq(TestTargetName), any[v5.MsgData])
        }
      }

      it("should not execute Comm open callbacks if the target is not found") {
        val message: ZMQMessage = kmBuilder
          .withHeader(CommOpen.toTypeString)
          .withContentString(CommOpen(TestCommId, TestTargetName, v5.MsgData.Empty))
          .build

        // Mark as target NOT being provided
        doReturn(None).when(spyCommStorage).getTargetCallbacks(anyString())

        // Simulate receiving a message from the kernel
        ioPubClient ! message

        // Check to see if "eventually" the callback is NOT triggered
        eventually {
          // Check that we have checked if the target exists
          verify(spyCommStorage).getTargetCallbacks(TestTargetName)

          verify(mockCommCallbacks, never()).executeOpenCallbacks(
            any[CommWriter], mockEq(TestCommId),
            mockEq(TestTargetName), any[v5.MsgData])
          verify(mockCommRegistrar, never()).link(TestTargetName, TestCommId)
        }
      }

      it("should execute all Comm msg callbacks on comm_msg message") {
        val message: ZMQMessage = kmBuilder
          .withHeader(CommMsg.toTypeString)
          .withContentString(CommMsg(TestCommId, v5.MsgData.Empty))
          .build

        // Mark as target being provided
        doReturn(Some(mockCommCallbacks)).when(spyCommStorage)
          .getCommIdCallbacks(any[v5.UUID])

        // Simulate receiving a message from the kernel
        ioPubClient ! message

        // Check to see if "eventually" the callback is triggered
        eventually {
          verify(mockCommCallbacks).executeMsgCallbacks(
            any[CommWriter], mockEq(TestCommId), any[v5.MsgData])
        }
      }

      it("should not execute Comm msg callbacks if the Comm id is not found") {
        val message: ZMQMessage = kmBuilder
          .withHeader(CommMsg.toTypeString)
          .withContentString(CommMsg(TestCommId, v5.MsgData.Empty))
          .build

        // Mark as target NOT being provided
        doReturn(None).when(spyCommStorage).getCommIdCallbacks(any[v5.UUID])

        // Simulate receiving a message from the kernel
        ioPubClient ! message

        // Check to see if "eventually" the callback is NOT triggered
        eventually {
          // Check that we have checked if the target exists
          verify(spyCommStorage).getCommIdCallbacks(TestCommId)

          verify(mockCommCallbacks, never()).executeMsgCallbacks(
            any[CommWriter], mockEq(TestCommId), any[v5.MsgData])
        }
      }

      it("should execute all Comm close callbacks on comm_close message") {
        val message: ZMQMessage = kmBuilder
          .withHeader(CommClose.toTypeString)
          .withContentString(CommClose(TestCommId, v5.MsgData.Empty))
          .build

        // Mark as target being provided
        doReturn(Some(mockCommCallbacks)).when(spyCommStorage)
          .getCommIdCallbacks(any[v5.UUID])

        // Simulate receiving a message from the kernel
        ioPubClient ! message

        // Check to see if "eventually" the callback is triggered
        eventually {
          verify(mockCommCallbacks).executeCloseCallbacks(
            any[CommWriter], mockEq(TestCommId), any[v5.MsgData])
        }
      }

      it("should not execute Comm close callbacks if Comm id is not found") {
        val message: ZMQMessage = kmBuilder
          .withHeader(CommClose.toTypeString)
          .withContentString(CommClose(TestCommId, v5.MsgData.Empty))
          .build

        // Mark as target NOT being provided
        doReturn(None).when(spyCommStorage).getCommIdCallbacks(any[v5.UUID])

        // Simulate receiving a message from the kernel
        ioPubClient ! message

        // Check to see if "eventually" the callback is NOT triggered
        eventually {
          // Check that we have checked if the target exists
          verify(spyCommStorage).getCommIdCallbacks(TestCommId)

          verify(mockCommCallbacks, never()).executeCloseCallbacks(
            any[CommWriter], mockEq(TestCommId), any[v5.MsgData])
        }
      }

      it("should call a registered callback on stream message") {
        val result = StreamContent("foo", "bar")
        val header = Header(id, "spark", id,
          MessageType.Outgoing.Stream.toString, "5.0")
        val parentHeader = Header(id, "spark", id,
          MessageType.Incoming.ExecuteRequest.toString, "5.0")

        val kernelMessage = new KernelMessage(
          Seq[Array[Byte]](),
          "",
          header,
          parentHeader,
          Metadata(),
          Json.toJson(result).toString()
        )
        val promise: Promise[String] = Promise()
        val de: DeferredExecution = DeferredExecution().onStream(
          (content: StreamContent) => {
            promise.success(content.text)
          }
        )
        DeferredExecutionManager.add(id, de)
        // Send the message to the IOPubClient
        val zmqMessage: ZMQMessage = kernelMessage

        ioPubClient ! zmqMessage

        whenReady(promise.future) {
          case res: String =>
            res shouldBe "bar"
          case _ =>
            fail(s"Received failure when asking IOPubClient")
        }
      }

      it("should not invoke callback when stream message's parent header is null") {
        // Construct the kernel message
        val result = StreamContent("foo", "bar")
        val header = Header(id, "spark", id,
          MessageType.Outgoing.Stream.toString, "5.0")

        val kernelMessage = new KernelMessage(
          Seq[Array[Byte]](),
          "",
          header,
          null,
          Metadata(),
          Json.toJson(result).toString()
        )

        // Send the message to the IOPubClient
        val zmqMessage: ZMQMessage = kernelMessage
        val futureResult: Future[Any] = ioPubClient.ask(zmqMessage)(TestTimeout)
        whenReady(futureResult) {
          case result: Failure[Any] =>
            //  Getting the value of the failure will cause the underlying exception will be thrown
            try {
              result.get
            } catch {
              case t:RuntimeException =>
                t.getMessage should be("Parent Header was null in Kernel Message.")
            }
          case result =>
            fail(s"Did not receive failure!! ${result}")
        }
      }
    }
  }
}
