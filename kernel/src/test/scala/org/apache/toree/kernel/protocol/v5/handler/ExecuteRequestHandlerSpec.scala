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

import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.apache.toree.kernel.api.{FactoryMethods, Kernel}
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content._
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.kernel.protocol.v5Test._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}
import play.api.libs.json.Json

import org.mockito.Mockito._
import org.mockito.Matchers._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import test.utils.MaxAkkaTestTimeout
class ExecuteRequestHandlerSpec extends TestKit(
  ActorSystem(
    "ExecuteRequestHandlerSpec",
    None,
    Some(org.apache.toree.Main.getClass.getClassLoader)
  )
) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar
  with BeforeAndAfter {

  private var mockActorLoader: ActorLoader = _
  private var mockFactoryMethods: FactoryMethods = _
  private var mockKernel: Kernel = _
  private var mockOutputStream: OutputStream = _
  private var handlerActor: ActorRef = _
  private var kernelMessageRelayProbe: TestProbe = _
  private var executeRequestRelayProbe: TestProbe = _
  private var statusDispatchProbe: TestProbe = _

  before {
    mockActorLoader = mock[ActorLoader]
    mockFactoryMethods = mock[FactoryMethods]
    mockKernel = mock[Kernel]
    mockOutputStream = mock[OutputStream]
    doReturn(mockFactoryMethods).when(mockKernel)
      .factory(any[KernelMessage], any[KMBuilder])

    doReturn(mockOutputStream).when(mockFactoryMethods)
      .newKernelOutputStream(anyString(), anyBoolean())

    // Add our handler and mock interpreter to the actor system
    handlerActor = system.actorOf(Props(
      classOf[ExecuteRequestHandler],
      mockActorLoader,
      mockKernel
    ))

    kernelMessageRelayProbe = new TestProbe(system)
    when(mockActorLoader.load(SystemActorType.KernelMessageRelay))
      .thenReturn(system.actorSelection(kernelMessageRelayProbe.ref.path.toString))

    executeRequestRelayProbe = new TestProbe(system)
    when(mockActorLoader.load(SystemActorType.ExecuteRequestRelay))
      .thenReturn(system.actorSelection(executeRequestRelayProbe.ref.path.toString))

    statusDispatchProbe = new TestProbe(system)
    when(mockActorLoader.load(SystemActorType.StatusDispatch))
      .thenReturn(system.actorSelection(statusDispatchProbe.ref.path.toString))
  }

  /**
   * This method simulates the interpreter passing back an
   * execute result and reply.
   */
  def replyToHandlerWithOkAndResult() = {
    //  This stubs the behaviour of the interpreter executing code
    val expectedClass = classOf[(ExecuteRequest, KernelMessage, OutputStream)]
    executeRequestRelayProbe.expectMsgClass(expectedClass)
    executeRequestRelayProbe.reply((
      ExecuteReplyOk(1, Some(Payloads()), Some(UserExpressions())),
      ExecuteResult(1, Data("text/plain" -> "resulty result"), Metadata())
    ))
  }
  
  def replyToHandlerWithOk() = {
    //  This stubs the behaviour of the interpreter executing code
    val expectedClass = classOf[(ExecuteRequest, KernelMessage, OutputStream)]
    executeRequestRelayProbe.expectMsgClass(expectedClass)
    executeRequestRelayProbe.reply((
      ExecuteReplyOk(1, Some(Payloads()), Some(UserExpressions())),
      ExecuteResult(1, Data("text/plain" -> ""), Metadata())
    ))
  }

  /**
   * This method simulates the interpreter passing back an
   * execute result and reply
   */
  def replyToHandlerWithErrorAndResult() = {
    //  This stubs the behaviour of the interpreter executing code
    val expectedClass = classOf[(ExecuteRequest, KernelMessage, OutputStream)]
    executeRequestRelayProbe.expectMsgClass(expectedClass)
    executeRequestRelayProbe.reply((
      ExecuteReplyError(1, Some(""), Some(""), Some(Nil)),
      ExecuteResult(1, Data("text/plain" -> "resulty result"), Metadata())
    ))
  }

  describe("ExecuteRequestHandler( ActorLoader )") {
    describe("#receive( KernelMessage ) when interpreter replies") {

      it("should send an execute result message if the result is not empty") {
        handlerActor ! MockExecuteRequestKernelMessage
        replyToHandlerWithOkAndResult()
        kernelMessageRelayProbe.fishForMessage(MaxAkkaTestTimeout) {
          case KernelMessage(_, _, header, _, _, _) =>
            header.msg_type == ExecuteResult.toTypeString
        }
      }

      it("should not send an execute result message if there is no result") {
        handlerActor ! MockExecuteRequestKernelMessage
        replyToHandlerWithOk()
        kernelMessageRelayProbe.fishForMessage(MaxAkkaTestTimeout) {
          case KernelMessage(_, _, header, _, _, _) =>
            header.msg_type != ExecuteResult.toTypeString
        }

      }

      it("should send an execute reply message") {
        handlerActor ! MockExecuteRequestKernelMessage
        replyToHandlerWithOkAndResult()
        kernelMessageRelayProbe.fishForMessage(MaxAkkaTestTimeout) {
          case KernelMessage(_, _, header, _, _, _) =>
            header.msg_type == ExecuteResult.toTypeString
        }
      }

      it("should send a status idle message after the reply and result") {
        handlerActor ! MockExecuteRequestKernelMessage
        replyToHandlerWithOkAndResult()

        val msgCount = new AtomicInteger(0)
        var statusMsgNum = -1
        var statusReceived = false

        val f1 = Future {
          kernelMessageRelayProbe.fishForMessage(MaxAkkaTestTimeout) {
            case KernelMessage(_, _, header, _, _, _) =>
              if (header.msg_type == ExecuteResult.toTypeString &&
                    !statusReceived)
                msgCount.incrementAndGet()
              else if (header.msg_type == ExecuteReply.toTypeString &&
                    !statusReceived)
                msgCount.incrementAndGet()
              statusReceived || (msgCount.get() >= 2)
          }
        }

        val f2 = Future {
          statusDispatchProbe.fishForMessage(MaxAkkaTestTimeout) {
            case (status, header) =>
              if (status == KernelStatusIdle.toString)
                statusReceived = true
                statusMsgNum = msgCount.get()
            statusReceived || (msgCount.get() >= 2)
          }
        }
        val fs = f1.zip(f2)
        Await.ready(fs, 3 * MaxAkkaTestTimeout)

        statusMsgNum should equal(2)
      }

      it("should send an execute input message") {
        handlerActor ! MockExecuteRequestKernelMessage
        kernelMessageRelayProbe.fishForMessage(MaxAkkaTestTimeout) {
          case KernelMessage(_, _, header, _, _, _) =>
            header.msg_type == ExecuteInput.toTypeString
        }
      }

      it("should send a message with ids equal to the incoming " +
        "KernelMessage's ids") {
        handlerActor ! MockExecuteRequestKernelMessage
        kernelMessageRelayProbe.fishForMessage(MaxAkkaTestTimeout) {
          case KernelMessage(ids, _, _, _, _, _) =>
            ids == MockExecuteRequestKernelMessage.ids
        }
      }

      it("should send a message with parent header equal to the incoming " +
        "KernelMessage's header") {
        handlerActor ! MockExecuteRequestKernelMessage
        kernelMessageRelayProbe.fishForMessage(MaxAkkaTestTimeout) {
          case KernelMessage(_, _, _, parentHeader, _, _) =>
            parentHeader == MockExecuteRequestKernelMessage.header
        }
      }

      // TODO: Investigate if this is still relevant at all
//      it("should send a status busy and idle message") {
//        handlerActor ! MockExecuteRequestKernelMessage
//        replyToHandlerWithOkAndResult()
//        var busy = false
//        var idle = false
//
//        statusDispatchProbe.receiveWhile(100.milliseconds) {
//          case Tuple2(status: KernelStatusType, header: Header)=>
//            if(status == KernelStatusType.Busy)
//              busy = true
//            if(status == KernelStatusType.Idle)
//              idle = true
//        }
//
//        idle should be (true)
//        busy should be (true)
//      }
    }
  }

  //  Testing error timeout for interpreter future
  describe("ExecuteRequestHandler( ActorLoader )") {
    describe("#receive( KernelMessage with bad JSON content )"){
      it("should respond with an execute_reply with status error")    {
        handlerActor ! MockKernelMessageWithBadExecuteRequest

        kernelMessageRelayProbe.fishForMessage(MaxAkkaTestTimeout) {
          // Only mark as successful if this specific message was received
          case KernelMessage(_, _, header, _, _, contentString)
            if header.msg_type == ExecuteReply.toTypeString =>
            val reply = Json.parse(contentString).as[ExecuteReply]
            reply.status == "error"
          case _ => false
        }
      }

      it("should send error message to relay") {
        handlerActor ! MockKernelMessageWithBadExecuteRequest

        kernelMessageRelayProbe.fishForMessage(MaxAkkaTestTimeout) {
          // Only mark as successful if this specific message was received
          case KernelMessage(_, _, header, _, _, _)
            if header.msg_type == ErrorContent.toTypeString => true
          case _ => false
        }
      }

      // TODO: Investigate if this is still relevant at all
//      it("should send a status idle message") {
//        handlerActor ! MockKernelMessageWithBadExecuteRequest
//        var busy = false
//        var idle = false
//
//        statusDispatchProbe.receiveWhile(100.milliseconds) {
//          case Tuple2(status: KernelStatusType, header: Header)=>
//            if(status == KernelStatusType.Busy)
//              busy = true
//            if(status == KernelStatusType.Idle)
//              idle = true
//        }
//
//        idle should be (true)
//        busy should be (false)
//      }
    }
  }
}
