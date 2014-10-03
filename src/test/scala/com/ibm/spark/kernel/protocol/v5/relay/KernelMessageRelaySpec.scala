package com.ibm.spark.kernel.protocol.v5.relay

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.ibm.spark.kernel.protocol.v5._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}
import org.mockito.Matchers.{eq => mockEq}
import org.mockito.AdditionalMatchers.{not => mockNot}

import scala.concurrent.duration._
import com.ibm.spark.kernel.protocol.v5.MessageType._
import com.ibm.spark.kernel.protocol.v5.KernelMessage
import scala.collection.immutable.HashMap
import com.ibm.spark.kernel.protocol.v5.KernelStatusType.KernelStatusType
import com.ibm.spark.kernel.protocol.v5.KernelStatusType
import akka.zeromq.ZMQMessage

class KernelMessageRelaySpec extends TestKit(ActorSystem("RelayActorSystem"))
  with ImplicitSender with FunSpecLike with Matchers with MockitoSugar
  with BeforeAndAfter {
  val IncomingMessageType = CompleteRequest.toString
  val OutgoingMessageType = CompleteReply.toString

  val header: Header = Header("<UUID>", "<USER>", "<SESSION>",
    "<TYPE>", "<VERSION>")
  val parentHeader: Header = Header("<PARENT-UUID>", "<PARENT-USER>",
    "<PARENT-SESSION>", "<PARENT-TYPE>", "<PARENT-VERSION>")
  val incomingKernelMessage: KernelMessage = KernelMessage(Seq("<ID>"),
    "<SIGNATURE>", header.copy(msg_type = IncomingMessageType),
    parentHeader, Metadata(), "<CONTENT>")
  val outgoingKernelMessage: KernelMessage = KernelMessage(Seq("<ID>"),
    "<SIGNATURE>", header.copy(msg_type = OutgoingMessageType),
    incomingKernelMessage.header, Metadata(), "<CONTENT>")

  var actorLoader: ActorLoader = _
  var signatureProbe: TestProbe = _
  var signatureSelection: ActorSelection = _
  var captureProbe: TestProbe = _
  var captureSelection: ActorSelection = _
  var handlerProbe: TestProbe = _
  var handlerSelection: ActorSelection = _
  var relayWithoutSignatureManager: ActorRef = _
  var relayWithSignatureManager: ActorRef = _

  def waitForStatusMessage(
    testProbe: TestProbe,
    status: KernelStatusType,
    relatedMessageHeader: Header
  ) = testProbe.expectMsg((status, relatedMessageHeader))

  def waitForBusyMessage(
    testProbe: TestProbe,
    relatedMessage: KernelMessage
  ) = waitForStatusMessage(
    testProbe, KernelStatusType.Busy, relatedMessage.header
  )

  def waitForIdleMessage(
    testProbe: TestProbe,
    relatedMessage: KernelMessage
  ) = waitForStatusMessage(
    testProbe, KernelStatusType.Idle, relatedMessage.parentHeader
  )

  before {
    // Create a mock ActorLoader for the Relay we are going to test
    actorLoader = mock[ActorLoader]

    // Create a probe for the signature manager and mock the ActorLoader to
    // return the associated ActorSelection
    signatureProbe = TestProbe()
    signatureSelection = system.actorSelection(signatureProbe.ref.path.toString)
    when(actorLoader.load(SystemActorType.SignatureManager))
      .thenReturn(signatureSelection)

    // Create a probe to capture output from the relay for testing
    captureProbe = TestProbe()
    captureSelection = system.actorSelection(captureProbe.ref.path.toString)
    when(actorLoader.load(mockNot(mockEq(SystemActorType.SignatureManager))))
      .thenReturn(captureSelection)

    relayWithoutSignatureManager = system.actorOf(Props(
      classOf[KernelMessageRelay], actorLoader, false
    ))

    relayWithSignatureManager = system.actorOf(Props(
      classOf[KernelMessageRelay], actorLoader, true
    ))
  }

  describe("Relay") {
    describe("#receive") {
      describe("when not using the signature manager") {
        it("should not send anything to SignatureManager for incoming") {
          relayWithoutSignatureManager ! true // Mark as ready for incoming
          relayWithoutSignatureManager ! incomingKernelMessage
          signatureProbe.expectNoMsg(25.millis)
        }

        it("should not send anything to SignatureManager for outgoing") {
          relayWithoutSignatureManager ! outgoingKernelMessage
          signatureProbe.expectNoMsg(25.millis)
        }

        // TODO: Investigate where the busy message went
        it("should relay KernelMessage for incoming") {
          val incomingMessage: ZMQMessage = incomingKernelMessage

          relayWithoutSignatureManager ! true // Mark as ready for incoming
          relayWithoutSignatureManager ! incomingMessage
          //waitForBusyMessage(captureProbe, incomingKernelMessage)
          captureProbe.expectMsg(incomingKernelMessage)
        }

        it("should relay KernelMessage for outgoing") {
          relayWithoutSignatureManager ! outgoingKernelMessage
          waitForIdleMessage(captureProbe, outgoingKernelMessage)
          captureProbe.expectMsg(outgoingKernelMessage)
        }
      }

      describe("when using the signature manager") {
        it("should verify the signature if the message is incoming") {
          relayWithSignatureManager ! true // Mark as ready for incoming
          relayWithSignatureManager ! incomingKernelMessage
          signatureProbe.expectMsg(incomingKernelMessage)
        }

        it("should construct the signature if the message is outgoing") {
          relayWithSignatureManager ! outgoingKernelMessage
          signatureProbe.expectMsg(outgoingKernelMessage)
        }
      }

      describe("when not ready") {
        it("should not relay the message if it is incoming") {
          val incomingMessage: ZMQMessage = incomingKernelMessage

          relayWithoutSignatureManager ! incomingMessage
          captureProbe.expectNoMsg(25.millis)
        }

        it("should relay the message if it is outgoing") {
          relayWithoutSignatureManager ! outgoingKernelMessage
          waitForIdleMessage(captureProbe, outgoingKernelMessage)
          captureProbe.expectMsg(outgoingKernelMessage)
        }
      }

      describe("when ready") {
        // TODO: Investigate where the busy message went
        it("should relay the message if it is incoming") {
          val incomingMessage: ZMQMessage = incomingKernelMessage

          relayWithoutSignatureManager ! true // Mark as ready for incoming
          relayWithoutSignatureManager ! incomingMessage
          //waitForBusyMessage(captureProbe, incomingKernelMessage)
          captureProbe.expectMsg(incomingKernelMessage)
        }

        it("should relay the message if it is outgoing") {
          relayWithoutSignatureManager ! true // Mark as ready for incoming
          relayWithoutSignatureManager ! outgoingKernelMessage
          waitForIdleMessage(captureProbe, outgoingKernelMessage)
          captureProbe.expectMsg(outgoingKernelMessage)
        }
      }
    }
  }
}
