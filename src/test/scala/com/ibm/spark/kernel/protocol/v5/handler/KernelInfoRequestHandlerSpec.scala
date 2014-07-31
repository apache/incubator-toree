package com.ibm.spark.kernel.protocol.v5.handler

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.ibm.spark.kernel.protocol.v5.content.KernelInfoReply
import com.ibm.spark.kernel.protocol.v5.{Header, KernelMessage}
import com.typesafe.config.ConfigFactory
import org.scalatest.{FunSpecLike, Matchers}
import play.api.libs.json.Json

import scala.concurrent.duration._

object KernelInfoRequestHandlerSpec {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class KernelInfoRequestHandlerSpec extends TestKit(
  ActorSystem("KernelInfoRequestHandlerSpec",
    ConfigFactory.parseString(KernelInfoRequestHandlerSpec.config))
) with ImplicitSender with FunSpecLike with Matchers {

  val actor = system.actorOf(Props(classOf[KernelInfoRequestHandler]))

  val header = Header("","","","","")
  val kernelMessage = new KernelMessage(
    Seq[String](), "test message", header, header, Map[String, String](), "{}"
  )

  describe("Kernel Info Request Handler") {
    it("should return a KernelMessage containing kernel info response") {
      actor ! kernelMessage
      val reply = receiveOne(10.seconds).asInstanceOf[KernelMessage]
      val kernelInfo = Json.parse(reply.contentString).as[KernelInfoReply]
      kernelInfo.implementation should be ("spark")
    }
  }
}
