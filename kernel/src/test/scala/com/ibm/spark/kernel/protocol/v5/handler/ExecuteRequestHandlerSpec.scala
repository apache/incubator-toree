/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.spark.kernel.protocol.v5.handler

import java.io.OutputStream

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.ibm.spark.kernel.protocol.v5.KernelStatusType.KernelStatusType
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content._
import com.ibm.spark.kernel.protocol.v5.kernel.ActorLoader
import com.ibm.spark.kernel.protocol.v5Test._
import org.apache.spark.sql.catalyst.expressions.IsNull
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}
import play.api.libs.json.Json

import scala.concurrent.duration._

class ExecuteRequestHandlerSpec extends TestKit(
  ActorSystem("ExecuteRequestHandlerSpec")
) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar
  with BeforeAndAfter {

  var actorLoader: ActorLoader = _
  var handlerActor: ActorRef = _
  var kernelMessageRelayProbe: TestProbe = _
  var executeRequestRelayProbe: TestProbe = _
  var statusDispatchProbe: TestProbe = _

  before {
    actorLoader = mock[ActorLoader]

    // Add our handler and mock interpreter to the actor system
    handlerActor = system.actorOf(Props(classOf[ExecuteRequestHandler], actorLoader))

    kernelMessageRelayProbe = new TestProbe(system)
    when(actorLoader.load(SystemActorType.KernelMessageRelay))
      .thenReturn(system.actorSelection(kernelMessageRelayProbe.ref.path.toString))

    executeRequestRelayProbe = new TestProbe(system)
    when(actorLoader.load(SystemActorType.ExecuteRequestRelay))
      .thenReturn(system.actorSelection(executeRequestRelayProbe.ref.path.toString))

    statusDispatchProbe = new TestProbe(system)
    when(actorLoader.load(SystemActorType.StatusDispatch))
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
        kernelMessageRelayProbe.fishForMessage(100.milliseconds) {
          case KernelMessage(_, _, header, _, _, _) =>
            header.msg_type == ExecuteResult.toTypeString
        }
      }

      it("should not send an execute result message if there is no result") {
        handlerActor ! MockExecuteRequestKernelMessage
        replyToHandlerWithOk()
        kernelMessageRelayProbe.fishForMessage(200.milliseconds) {
          case KernelMessage(_, _, header, _, _, _) =>
            header.msg_type != ExecuteResult.toTypeString
        }

      }

      it("should send an execute reply message") {
        handlerActor ! MockExecuteRequestKernelMessage
        replyToHandlerWithOkAndResult()
        kernelMessageRelayProbe.fishForMessage(200.milliseconds) {
          case KernelMessage(_, _, header, _, _, _) =>
            header.msg_type == ExecuteResult.toTypeString
        }
      }

      it("should send an execute input message") {
        handlerActor ! MockExecuteRequestKernelMessage
        kernelMessageRelayProbe.fishForMessage(200.milliseconds) {
          case KernelMessage(_, _, header, _, _, _) =>
            header.msg_type == ExecuteInput.toTypeString
        }
      }

      it("should send a message with ids equal to the incoming " +
        "KernelMessage's ids") {
        handlerActor ! MockExecuteRequestKernelMessage
        kernelMessageRelayProbe.fishForMessage(200.milliseconds) {
          case KernelMessage(ids, _, _, _, _, _) =>
            ids == MockExecuteRequestKernelMessage.ids
        }
      }

      it("should send a message with parent header equal to the incoming " +
        "KernelMessage's header") {
        handlerActor ! MockExecuteRequestKernelMessage
        kernelMessageRelayProbe.fishForMessage(200.milliseconds) {
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

        kernelMessageRelayProbe.fishForMessage(200.milliseconds) {
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

        kernelMessageRelayProbe.fishForMessage(200.milliseconds) {
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
