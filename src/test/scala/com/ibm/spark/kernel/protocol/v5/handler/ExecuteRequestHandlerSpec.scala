package com.ibm.spark.kernel.protocol.v5.handler

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5Test._
import com.ibm.spark.kernel.protocol.v5.content._
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
  }

  /**
   * This method simulates the interpreter passing back an
   * execute result and reply
   */
  def replyToHandler(){
    //  This stubs the behaviour of the interpreter executing code
    executeRequestRelayProbe.expectMsgClass(classOf[ExecuteRequest])
    executeRequestRelayProbe.reply((
      ExecuteReplyOk(1, Some(Payloads()), Some(UserExpressions())),
      ExecuteResult(1, Data("text/plain" -> ""), Metadata())
      ))
  }

  describe("ExecuteRequestHandler( ActorLoader )") {
    describe("#receive( KernelMessage )") {

      it("should send an execute result message") {
        handlerActor ! MockExecuteRequestKernelMessage
        replyToHandler()
        var executeResultMessage: KernelMessage = null
        kernelMessageRelayProbe.receiveWhile(1.second) {
          case message : KernelMessage =>
            if(message.header.msg_type == MessageType.ExecuteResult.toString)
              executeResultMessage = message
        }
        executeResultMessage should not be(null)
      }

      it("should send an execute reply message") {
        handlerActor ! MockExecuteRequestKernelMessage
        replyToHandler()
        var executeReplyMessage: KernelMessage = null
        kernelMessageRelayProbe.receiveWhile(1.second) {
          case message : KernelMessage =>
            if(message.header.msg_type == MessageType.ExecuteReply.toString)
              executeReplyMessage = message
        }
        executeReplyMessage should not be(null)
      }

      it("should send an execute input message") {
        handlerActor ! MockExecuteRequestKernelMessage
        var executeInputMessage: KernelMessage = null
        kernelMessageRelayProbe.receiveWhile(1.second) {
          case message : KernelMessage =>
            if(message.header.msg_type == MessageType.ExecuteInput.toString)
              executeInputMessage = message
        }
        executeInputMessage should not be(null)
      }
    }
  }

  //  Testing error timeout for interpreter future
  describe("ExecuteRequestHandler( ActorLoader )") {
    describe("#receive( KernelMessage )") {
      it("should send execute_input message to the ") {
        //  ExecuteInput
        handlerActor ! MockExecuteRequestKernelMessage
        var executeInputMessage: KernelMessage = null
        kernelMessageRelayProbe.receiveWhile(1.second) {
          case message : KernelMessage =>
            if(message.header.msg_type == MessageType.ExecuteInput.toString)
              executeInputMessage = message
        }
        executeInputMessage should not be(null)
      }

      it("should send an ExecuteRequest to ExecuteRequestRelay") {
        handlerActor ! MockExecuteRequestKernelMessage
        executeRequestRelayProbe.expectMsg(MockExecuteRequest)
      }

      it("interpreter does not reply and relay should receive an execute reply of type error") {
        handlerActor ! MockExecuteRequestKernelMessage
        var executeReplyMessage: KernelMessage = null
        kernelMessageRelayProbe.receiveWhile(1500.millis) {
          case message : KernelMessage =>
            if(message.header.msg_type == MessageType.ExecuteReply.toString)
              executeReplyMessage = message
        }
        executeReplyMessage.header.msg_type should be(
          MessageType.ExecuteReply.toString
        )

        val replyContent =
          Json.parse(executeReplyMessage.contentString).as[ExecuteReply]
        replyContent.status should be("error")
      }

      it("interpreter does not reply and relay should receive an error message") {
        handlerActor ! MockExecuteRequestKernelMessage
        var errorContentMessage: KernelMessage = null
        kernelMessageRelayProbe.receiveWhile(1500.millis) {
          case message : KernelMessage =>
            if(message.header.msg_type == MessageType.Error.toString)
              errorContentMessage = message
        }

        val errorContent =
          Json.parse(errorContentMessage.contentString).as[ErrorContent]
        errorContent should not be(null)
      }
    }
  }

  //  Testing error timeout for interpreter future
  describe("ExecuteRequestHandler( ActorLoader )") {
    describe("#receive( KernelMessage with bad JSON content )"){
      it("should respond with an execute_reply with status error")    {
        handlerActor ! MockKernelMessageWithBadExecuteRequest
        var executeReplyMessage: KernelMessage = null
        kernelMessageRelayProbe.receiveWhile(1500.millis) {
          case message : KernelMessage =>
            if(message.header.msg_type == MessageType.ExecuteReply.toString)
              executeReplyMessage = message
        }
        executeReplyMessage.header.msg_type should be(
          MessageType.ExecuteReply.toString
        )

        val replyContent =
          Json.parse(executeReplyMessage.contentString).as[ExecuteReply]
        replyContent.status should be("error")
      }

      it("should send error message to relay") {
        handlerActor ! MockKernelMessageWithBadExecuteRequest
        var errorContentMessage: KernelMessage = null
        kernelMessageRelayProbe.receiveWhile(1500.millis) {
          case message : KernelMessage =>
            if(message.header.msg_type == MessageType.Error.toString)
              errorContentMessage = message
        }

        val errorContent =
          Json.parse(errorContentMessage.contentString).as[ErrorContent]
        errorContent should not be(null)
      }
    }
  }
}
