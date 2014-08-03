package integration.security

import java.io.ByteArrayOutputStream

import akka.actor.Status.Success
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.ibm.spark.interpreter.ScalaInterpreter
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content._
import com.ibm.spark.kernel.protocol.v5.interpreter.tasks.InterpreterTaskFactory
import com.ibm.spark.kernel.protocol.v5.security.SignatureCheckerActor
import com.typesafe.config.ConfigFactory
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}
import org.slf4j.Logger

import akka.pattern.ask
import scala.concurrent.duration._

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
    "36ddda9343b2510338a3210f1c14e95977a1e5a77cd9188476575fcb381760b9"
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
