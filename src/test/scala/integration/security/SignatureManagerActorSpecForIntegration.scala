package integration.security

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.security.SignatureManagerActor
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}

import scala.concurrent.duration._

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

  private val sigKey = "12345"
  private val signature =
    "1c4859a7606fd93eb5f73c3d9642f9bc860453ba42063961a00d02ed820147b5"
  private val goodMessage =
    KernelMessage(
      List(), signature,
      Header("a", "b", "c", "d", "e"),
      ParentHeader("f", "g", "h", "i", "j"),
      Metadata(),
      "<STRING>"
    )
  private val badMessage =
    KernelMessage(
      List(), "wrong signature",
      Header("a", "b", "c", "d", "e"),
      ParentHeader("f", "g", "h", "i", "j"),
      Metadata(),
      "<STRING>"
    )

  private var signatureManager: ActorRef = _

  before {
    signatureManager =
      system.actorOf(Props(classOf[SignatureManagerActor], sigKey))

  }

  after {
    signatureManager = null
  }

  describe("SignatureManagerActor") {
    describe("#receive") {
      describe("when receiving a ZMQMessage instance") {
        it("should return true if the signature is valid") {
          val zmqMessage: ZMQMessage = goodMessage
          signatureManager ! zmqMessage
          expectMsg(true)
        }

        it("should return false if the signature is invalid") {
          val zmqMessage: ZMQMessage = badMessage
          signatureManager ! zmqMessage
          expectMsg(false)
        }
      }

      describe("when receiving a KernelMessage instance") {
        it("should insert a valid signature into the message and return it") {
          signatureManager ! badMessage

          val newKernelMessage =
            receiveOne(5.seconds).asInstanceOf[KernelMessage]

          newKernelMessage.signature should be (signature)
        }
      }
    }
  }
}
