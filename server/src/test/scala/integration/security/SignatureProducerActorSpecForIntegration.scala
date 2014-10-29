package integration.security

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.security.SignatureProducerActor
import com.ibm.spark.security.Hmac
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}

object SignatureProducerActorSpecForIntegration {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class SignatureProducerActorSpecForIntegration extends TestKit(
  ActorSystem(
    "SignatureProducerActorSpec",
    ConfigFactory.parseString(SignatureProducerActorSpecForIntegration.config)
  )
) with ImplicitSender with FunSpecLike with Matchers with BeforeAndAfter
{

  private val sigKey = "12345"

  private var signatureProducer: ActorRef = _

  before {
    val hmac = Hmac(sigKey)
    signatureProducer =
      system.actorOf(Props(classOf[SignatureProducerActor], hmac))

  }

  after {
    signatureProducer = null
  }

  describe("SignatureProducerActor") {
    describe("#receive") {
      it("should return the correct signature for a kernel message") {
        val expectedSignature =
          "1c4859a7606fd93eb5f73c3d9642f9bc860453ba42063961a00d02ed820147b5"
        val message =
          KernelMessage(
            null, "",
            Header("a", "b", "c", "d", "e"),
            ParentHeader("f", "g", "h", "i", "j"),
            Metadata(),
            "<STRING>"
          )

        signatureProducer ! message
        expectMsg(expectedSignature)
      }
    }
  }
}
