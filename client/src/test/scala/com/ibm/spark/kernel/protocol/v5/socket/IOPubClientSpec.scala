package com.ibm.spark.kernel.protocol.v5.socket

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.client.message.StreamMessage
import com.ibm.spark.kernel.protocol.v5.content.{StreamContent, ExecuteResult}
import com.ibm.spark.kernel.protocol.v5.Utilities._
import com.typesafe.config.ConfigFactory
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpecLike}
import org.mockito.Mockito._
import org.mockito.Matchers._
import play.api.libs.json.Json

object IOPubClientSpec {
  val config ="""
    akka {
      loglevel = "WARNING"
    }"""
}

class IOPubClientSpec extends TestKit(ActorSystem(
  "IOPubClientSpecSystem", ConfigFactory.parseString(IOPubClientSpec.config)
)) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar
{
  describe("IOPubClient( ClientSocketFactory )") {
    //  Create a probe for the socket
    val clientSocketProbe = TestProbe()
    //  Mock the socket factory
    val mockClientSocketFactory = mock[ClientSocketFactory]
    //  Stub the return value for the socket factory
    when(mockClientSocketFactory.IOPubClient(anyObject(), any[ActorRef])).thenReturn(clientSocketProbe.ref)

    //  Construct the object we will test against
    val ioPubClient = system.actorOf(Props(classOf[IOPubClient], mockClientSocketFactory))

    describe("#receive( ZMQMessage )") {
      it("should send an ExecuteResult message") {
        //  Generate an id for the kernel message
        val id = UUID.randomUUID().toString

        //  Construct the kernel message
        val result = ExecuteResult(1, Data(), Metadata())
        val header = Header(UUID.randomUUID().toString, "spark", UUID.randomUUID().toString, MessageType.ExecuteResult.toString, "5.0")
        val parentHeader = Header(id, "spark", UUID.randomUUID().toString, MessageType.ExecuteRequest.toString, "5.0")
        val kernelMessage = new KernelMessage(
          Seq[String](), "",
          header, parentHeader,
          Metadata(), Json.toJson(result).toString()
        )

        // Register ourselves to receive the ExecuteResult from the IOPubClient
        ioPubClient ! id

        // Send the message to the IOPubClient
        val zmqMessage: ZMQMessage = kernelMessage
        ioPubClient ! zmqMessage

        Thread.sleep(10)
        expectMsg(result)
      }

      it("should call a registered callback on stream message") {
        // Generate an id for the kernel message
        val id = UUID.randomUUID().toString

        // Callback and method of verifying the callback was called
        var capturedArg: Any = None
        val func = (x: Any) => { capturedArg = x }

        // Construct the kernel message
        val result = StreamContent("foo", "bar")
        val header = Header(id, "spark", id, MessageType.Stream.toString, "5.0")
        val parentHeader = Header(id, "spark", id, MessageType.ExecuteRequest.toString, "5.0")

        val kernelMessage = new KernelMessage(
          Seq[String](),
          "",
          header,
          parentHeader,
          Metadata(),
          Json.toJson(result).toString()
        )

        // Register the callback with the IOPubClient
        ioPubClient ! StreamMessage(parentHeader.msg_id, func)

        // Send the message to the IOPubClient
        val zmqMessage: ZMQMessage = kernelMessage
        ioPubClient ! zmqMessage

        // Verify callback was called
        Thread.sleep(10)
        capturedArg shouldNot be(None)
      }
    }
  }
}
