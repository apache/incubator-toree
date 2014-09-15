package com.ibm.spark.kernel.protocol.v5.handler

import java.io.OutputStream

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.ibm.spark.kernel.protocol.v5.KernelStatusType.KernelStatusType
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content._
import com.ibm.spark.kernel.protocol.v5Test._
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

    kernelMessageRelayProbe = TestProbe()
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
    val expectedClass = classOf[(ExecuteRequest, OutputStream)]
    executeRequestRelayProbe.expectMsgClass(expectedClass)
    executeRequestRelayProbe.reply((
      ExecuteReplyOk(1, Some(Payloads()), Some(UserExpressions())),
      ExecuteResult(1, Data("text/plain" -> "resulty result"), Metadata())
    ))
  }
  
  def replyToHandlerWithOk() = {
    //  This stubs the behaviour of the interpreter executing code
    val expectedClass = classOf[(ExecuteRequest, OutputStream)]
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
    val expectedClass = classOf[(ExecuteRequest, OutputStream)]
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
        var executeResultMessage: KernelMessage = null
        kernelMessageRelayProbe.receiveWhile(100.milliseconds) {
          case message : KernelMessage =>
            if(message.header.msg_type == MessageType.ExecuteResult.toString)
              executeResultMessage = message
        }
        executeResultMessage should not be(null)
      }

      it("should not send an execute result message if there is no result") {
        handlerActor ! MockExecuteRequestKernelMessage
        replyToHandlerWithOk()
        var executeResultMessage: KernelMessage = null
        kernelMessageRelayProbe.receiveWhile(100.milliseconds) {
          case message : KernelMessage =>
            if(message.header.msg_type == MessageType.ExecuteResult.toString)
              executeResultMessage = message
        }
        executeResultMessage should be(null)
      }

      it("should send an execute reply message") {
        handlerActor ! MockExecuteRequestKernelMessage
        replyToHandlerWithOkAndResult()
        var executeReplyMessage: KernelMessage = null
        kernelMessageRelayProbe.receiveWhile(100.milliseconds) {
          case message : KernelMessage =>
            if(message.header.msg_type == MessageType.ExecuteReply.toString)
              executeReplyMessage = message
        }
        executeReplyMessage should not be(null)
      }

      it("should send an execute input message") {
        handlerActor ! MockExecuteRequestKernelMessage
        var executeInputMessage: KernelMessage = null
        kernelMessageRelayProbe.receiveWhile(100.milliseconds) {
          case message : KernelMessage =>
            if(message.header.msg_type == MessageType.ExecuteInput.toString)
              executeInputMessage = message
        }
        executeInputMessage should not be(null)
      }

      it("should send a status busy and idle message") {
        handlerActor ! MockExecuteRequestKernelMessage
        replyToHandlerWithOkAndResult()
        var busy = false
        var idle = false

        statusDispatchProbe.receiveWhile(100.milliseconds) {
          case Tuple2(status: KernelStatusType, header: Header)=>
            if(status == KernelStatusType.Busy)
              busy = true
            if(status == KernelStatusType.Idle)
              idle = true
        }

        idle should be (true)
        busy should be (true)
      }
    }
  }

  //  Testing error timeout for interpreter future
  describe("ExecuteRequestHandler( ActorLoader )") {
    describe("#receive( KernelMessage with bad JSON content )"){
      it("should respond with an execute_reply with status error")    {
        handlerActor ! MockKernelMessageWithBadExecuteRequest
        var executeReplyMessage: Option[KernelMessage] = None
        kernelMessageRelayProbe.receiveWhile(100.milliseconds) {
          case message: KernelMessage =>
            if(message.header.msg_type == MessageType.ExecuteReply.toString)
              executeReplyMessage = Option(message)
        }

        executeReplyMessage match {
          case None =>
            fail("Expected an ExecuteReply message from KernelMessageRelay")
          case Some(message) =>
            message.header.msg_type should be(MessageType.ExecuteReply.toString)
            val reply = Json.parse(message.contentString).as[ExecuteReply]
            reply.status should be("error")
        }
      }

      it("should send error message to relay") {
        handlerActor ! MockKernelMessageWithBadExecuteRequest
        var errorContentMessage: Option[KernelMessage] = None

        kernelMessageRelayProbe.receiveWhile(100.milliseconds) {
          case message: KernelMessage =>
            if(message.header.msg_type == MessageType.Error.toString)
              errorContentMessage = Option(message)
        }

        errorContentMessage match {
          case None =>
            fail("Expected an ExecuteReply message from KernelMessageRelay")
          case Some(err) =>
            err.header.msg_type should be("error")
        }
      }

      it("should send a status idle message") {
        handlerActor ! MockKernelMessageWithBadExecuteRequest
        var busy = false
        var idle = false

        statusDispatchProbe.receiveWhile(100.milliseconds) {
          case Tuple2(status: KernelStatusType, header: Header)=>
            if(status == KernelStatusType.Busy)
              busy = true
            if(status == KernelStatusType.Idle)
              idle = true
        }

        idle should be (true)
        busy should be (false)
      }
    }
  }
}
