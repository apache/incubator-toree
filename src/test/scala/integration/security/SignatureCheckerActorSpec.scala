package integration.security

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.security.SignatureCheckerActor
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}

object SignatureCheckerActorSpec {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class SignatureCheckerActorSpec extends TestKit(
  ActorSystem(
    "SignatureCheckerActorSpec",
    ConfigFactory.parseString(SignatureCheckerActorSpec.config)
  )
) with ImplicitSender with FunSpecLike with Matchers with BeforeAndAfter
{

  private val sigKey = "12345"
  private val signature =
    "1c4859a7606fd93eb5f73c3d9642f9bc860453ba42063961a00d02ed820147b5"
  private val goodMessage =
    KernelMessage(
      null, signature,
      Header("a", "b", "c", "d", "e"),
      ParentHeader("f", "g", "h", "i", "j"),
      Metadata(),
      "<STRING>"
    )
  private val badMessage =
    KernelMessage(
      null, "wrong signature",
      Header("a", "b", "c", "d", "e"),
      ParentHeader("f", "g", "h", "i", "j"),
      Metadata(),
      "<STRING>"
    )

  private var signatureChecker: ActorRef = _

  before {
    signatureChecker =
      system.actorOf(Props(classOf[SignatureCheckerActor], sigKey))

  }

  after {
    signatureChecker = null
  }

  describe("SignatureCheckerActor") {
    describe("#receive") {
      it("should return true if the kernel message is valid") {
        signatureChecker ! goodMessage
        expectMsg(true)
      }

      it("should return false if the kernel message is invalid") {
        signatureChecker ! badMessage
        expectMsg(false)
      }
    }
  }
}
