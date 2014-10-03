package integration.security

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.security.SignatureManagerActor
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}

import scala.concurrent.duration._
import scala.collection.immutable.HashMap
import com.ibm.spark.kernel.protocol.v5.MessageType._
import com.ibm.spark.kernel.protocol.v5.KernelMessage
import play.api.libs.json.Json

object SignatureManagerActorSpecForIntegration {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class SignatureManagerActorSpecForIntegration extends TestKit(
  ActorSystem(
    "SignatureManagerActorSpec",
    ConfigFactory.parseString(SignatureManagerActorSpecForIntegration.config)
  )
) with ImplicitSender with FunSpecLike with Matchers with BeforeAndAfter
{
  private val IncomingMessageType = "d" // Needed for valid signature
  
  private val sigKey = "12345"
  private val signature =
    "1c4859a7606fd93eb5f73c3d9642f9bc860453ba42063961a00d02ed820147b5"
  private val goodIncomingMessage =
    KernelMessage(
      List(), signature,
      Header("a", "b", "c", IncomingMessageType, "e"),
      ParentHeader("f", "g", "h", "i", "j"),
      Metadata(),
      "<STRING>"
    )
  private val badIncomingMessage =
    KernelMessage(
      List(), "wrong signature",
      Header("a", "b", "c", IncomingMessageType, "e"),
      ParentHeader("f", "g", "h", "i", "j"),
      Metadata(),
      "<STRING>"
    )

  private var signatureManager: ActorRef = _
  private var signatureManagerWithNoIncoming: ActorRef = _

  before {
    signatureManager =
      system.actorOf(Props(
        classOf[SignatureManagerActor], sigKey
      ))

    signatureManagerWithNoIncoming =
      system.actorOf(Props(
        classOf[SignatureManagerActor], sigKey
      ))
  }

  after {
    signatureManager = null
  }

  describe("SignatureManagerActor") {
    describe("#receive") {
      describe("when receiving an incoming message") {
        it("should return true if the signature is valid") {
          val blob =
            Json.stringify(Json.toJson(goodIncomingMessage.header)) ::
            Json.stringify(Json.toJson(goodIncomingMessage.parentHeader)) ::
            Json.stringify(Json.toJson(goodIncomingMessage.metadata)) ::
            goodIncomingMessage.contentString ::
            Nil
          signatureManager ! ((goodIncomingMessage.signature, blob))
          expectMsg(true)
        }

        it("should return false if the signature is invalid") {
          val blob =
            Json.stringify(Json.toJson(badIncomingMessage.header)) ::
            Json.stringify(Json.toJson(badIncomingMessage.parentHeader)) ::
            Json.stringify(Json.toJson(badIncomingMessage.metadata)) ::
            badIncomingMessage.contentString ::
            Nil
          signatureManager ! ((badIncomingMessage.signature, blob))
          expectMsg(false)
        }
      }

      describe("when receiving an outgoing message") {
        it("should insert a valid signature into the message and return it") {
          // Sending to signature manager that has no incoming messages
          signatureManagerWithNoIncoming ! badIncomingMessage

          val newKernelMessage =
            receiveOne(5.seconds).asInstanceOf[KernelMessage]

          newKernelMessage.signature should be (signature)
        }
      }
    }
  }
}
