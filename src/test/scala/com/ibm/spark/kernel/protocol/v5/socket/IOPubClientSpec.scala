package com.ibm.spark.kernel.protocol.v5.socket

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.ExecuteResult
import com.typesafe.config.ConfigFactory
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}
import play.api.libs.json.Json

class IOPubClientSpec extends TestKit(ActorSystem("IOPubClientSpecSystem", ConfigFactory.parseString(ShellSpec.config)))
with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {
  describe("IOPubClient( SocketFactory )") {
    //  Create a probe for the socket
    val clientSocketProbe = TestProbe()
    //  Mock the socket factory
    val mockSocketFactory = mock[SocketFactory]
    //  Stub the return value for the socket factory
    when(mockSocketFactory.IOPubClient(anyObject(), any[ActorRef])).thenReturn(clientSocketProbe.ref)

    //  Construct the object we will test against
    val ioPubClient = system.actorOf(Props(classOf[IOPubClient], mockSocketFactory))

    describe("#receive( ZMQMessage )") {
      //  Generate an id for the kernel message
      val id = UUID.randomUUID().toString

      //  Construct the kernel message
      val result = ExecuteResult(1, Data(), Metadata())
      val header = Header(id, "spark", UUID.randomUUID().toString, MessageType.ExecuteResult.toString, "5.0")
      val kernelMessage = new KernelMessage(Seq[String](), "", header, DefaultHeader, Metadata(), Json.toJson(result).toString())

      // Register ourselves to receive the ExecuteResult from the IOPubClient
      ioPubClient ! id

      // Send the message to the IOPubClient
      val zmqMessage: ZMQMessage = kernelMessage
      ioPubClient ! zmqMessage

      Thread.sleep(1000)
      it("should receive an ExecuteResult message") {
        expectMsg(result)
      }
    }
  }
}
