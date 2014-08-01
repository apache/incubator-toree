package com.ibm.spark.kernel.protocol.v5.handler

import akka.actor.{ActorSelection, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.ibm.spark.kernel.protocol.v5.content.ExecuteRequest
import com.ibm.spark.kernel.protocol.v5.{ActorLoader, Header, KernelMessage}
import com.typesafe.config.ConfigFactory
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}

import scala.concurrent.duration._

object ExecuteRequestHandlerSpec {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class ExecuteRequestHandlerSpec extends TestKit(
  ActorSystem("KernelInfoRequestHandlerSpec",
    ConfigFactory.parseString(ExecuteRequestHandlerSpec.config))
) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {

  val actorLoader : ActorLoader = mock[ActorLoader]
  val probe : TestProbe = TestProbe()
  val mockSelection: ActorSelection = system.actorSelection(probe.ref.path.toString)
  when(actorLoader.loadInterpreterActor()).thenReturn(mockSelection)

  val actor = system.actorOf(Props(classOf[ExecuteRequestHandler], actorLoader))

  val header = Header("","","","","")
  val kernelMessage = new KernelMessage(
    Seq[String](), "test message", header, header, Map[String, String](),
      """{
        "code": "spark code",
        "silent": false,
        "store_history": true,
        "user_expressions": {},
        "allow_stdin": false
      }"""
  )

  describe("Execute Request Handler") {
    it("should return a KernelMessage containing execute request") {
      actor ! kernelMessage
      val executeRequest = probe.receiveOne(10.seconds).asInstanceOf[ExecuteRequest]
      executeRequest.code should be ("spark code")
    }
  }
}
