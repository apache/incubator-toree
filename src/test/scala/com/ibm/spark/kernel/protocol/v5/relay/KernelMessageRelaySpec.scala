package com.ibm.spark.kernel.protocol.v5.relay

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}

import scala.concurrent.duration._

class KernelMessageRelaySpec extends TestKit(ActorSystem("RelayActorSystem"))
with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {
  //  The header for the message
  val header: Header = Header("<UUID>", "<USER>", "<SESSION>",
    MessageType.ClearOutput.toString, "<VERSION>")
  //  The parent header for the message
  val parentHeader: Header = Header("<PARENT-UUID>", "<PARENT-USER>", "<PARENT-SESSION>",
    MessageType.ClearOutput.toString, "<PARENT-VERSION>")
  //  The actual kernel message
  val kernelMessage: KernelMessage = KernelMessage(Seq("<ID>"), "<SIGNATURE>", header,
    parentHeader, Metadata(), "<CONTENT>")
  //  Use the implicit to convert the KernelMessage to ZMQMessage
  val zmqMessage: ZMQMessage = kernelMessage

  describe("Relay( ActorLoader, false )") {
    //  Create a mock ActorLoader for the Relay we are going to test
    val actorLoader: ActorLoader = mock[ActorLoader]

    //  Create a probe for the signature manager and mock the ActorLoader to return the associated ActorSelection
    val signatureProbe: TestProbe = TestProbe()
    val signatureSelection: ActorSelection = system.actorSelection(signatureProbe.ref.path.toString)
    when(actorLoader.load(SystemActorType.SignatureManager)).thenReturn(signatureSelection)

    //  Create a probe for the handler and mock the ActorLoader to return the associated ActorSelection
    val handlerProbe: TestProbe = TestProbe()
    val handlerSelection: ActorSelection = system.actorSelection(handlerProbe.ref.path.toString)
    when(actorLoader.load(MessageType.ClearOutput)).thenReturn(handlerSelection)

    //  The relay we will test against
    val relay: ActorRef = system.actorOf(Props(classOf[KernelMessageRelay], actorLoader, false))

    describe("#receive( KernelMessage )") {
      relay ! kernelMessage
      it("should not send anything to SecurityManager ") {
        signatureProbe.expectNoMsg(500.millis)
      }
      it("should relay KernelMessage") {
        handlerProbe.expectMsg(kernelMessage)
      }
    }
    describe("#receive( ZMQMessage )") {
      relay ! zmqMessage
      it("should relay ZMQMessage as KernelMessage") {
        handlerProbe.expectMsg(kernelMessage)
      }
    }
  }

  describe("Relay( ActorLoader, true )"){
    //  Create a mock ActorLoader for the Relay we are going to test
    val actorLoader: ActorLoader = mock[ActorLoader]

    //  Create a probe for the signature manager and mock the ActorLoader to return the associated ActorSelection
    val signatureProbe: TestProbe = TestProbe()
    val signatureSelection: ActorSelection = system.actorSelection(signatureProbe.ref.path.toString)
    when(actorLoader.load(SystemActorType.SignatureManager)).thenReturn(signatureSelection)

    //  Create a probe for the handler and mock the ActorLoader to return the associated ActorSelection
    val handlerProbe: TestProbe = TestProbe()
    val handlerSelection: ActorSelection = system.actorSelection(handlerProbe.ref.path.toString)
    when(actorLoader.load(MessageType.ClearOutput)).thenReturn(handlerSelection)

    //  The Relay we are going to be testing against
    val relay: ActorRef = system.actorOf(Props(classOf[KernelMessageRelay], actorLoader, true))

    describe("#receive( KernelMessage )") {
      relay ! kernelMessage

      it("should send KernelMessage to security manager") {
        signatureProbe.expectMsg(kernelMessage)
        signatureProbe.reply(kernelMessage)

      }

      it("should send KernelMessage to handler"){
        handlerProbe.expectMsg(kernelMessage)
      }
    }

    describe("#receive( ZMQMessage )") {
      describe("when SecurityManager#( ZMQMessage ) returns true") {
        relay ! zmqMessage

        it("should send ZMQMessage to security manager") {
          signatureProbe.expectMsg(zmqMessage)
          signatureProbe.reply(true)
        }

        it("should send KernelMessage to handler"){
          handlerProbe.expectMsg(kernelMessage)
        }
      }

      describe("when SecurityManager#( ZMQMessage ) returns false") {
        relay ! zmqMessage

        it("should send ZMQMessage to security manager") {
          signatureProbe.expectMsg(zmqMessage)
          signatureProbe.reply(false)
        }

        it("should not send KernelMessage to handler"){
          handlerProbe.expectNoMsg(500.millis)
        }
      }
    }
  }
}
