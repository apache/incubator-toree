package com.ibm.spark.kernel.protocol.v5.socket

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.ExecuteRequest
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpecLike}
import org.mockito.Mockito._
import org.mockito.Matchers._
import play.api.libs.json.Json

class ShellClientSpec extends TestKit(ActorSystem("ShellActorSpec"))
  with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {

  describe("ShellClientActor") {
    val socketFactory = mock[SocketFactory]
    val probe : TestProbe = TestProbe()
    when(socketFactory.ShellClient(
      any(classOf[ActorSystem]), any(classOf[ActorRef])
    )).thenReturn(probe.ref)

    val shellClient = system.actorOf(Props(classOf[ShellClient], socketFactory))

    describe("send execute request") {
      it("should send execute request") {
        val request = ExecuteRequest(
          "foo", false, true, UserExpressions(), true
        )
        val header = Header(
          UUID.randomUUID().toString, "spark",
          UUID.randomUUID().toString, MessageType.ExecuteRequest.toString,
          "5.0"
        )
        val kernelMessage = KernelMessage(
          Seq[String](), "",
          header, HeaderBuilder.empty,
          Metadata(), Json.toJson(request).toString
        )
        shellClient ! kernelMessage
        probe.expectMsgClass(classOf[ZMQMessage])
      }
    }
  }
}