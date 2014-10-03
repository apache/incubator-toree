package integration.socket

import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.ExecuteResult
import com.ibm.spark.kernel.protocol.v5.socket._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}
import play.api.libs.json.Json

import scala.concurrent.duration._

class ClientToIOPubSpecForIntegration extends TestKit(ActorSystem("IOPubSystem"))
with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {
  describe("Client-IOPub Integration"){
    describe("Client"){
      it("should connect to IOPub Socket"){
        // setup
        val mockSocketFactory = mock[SocketFactory]
        val iopubProbe = TestProbe()
        when(mockSocketFactory.IOPub(any[ActorSystem]))
          .thenReturn(iopubProbe.ref)

        // Server and client sockets
        val ioPub = system.actorOf(
          Props(classOf[IOPub], mockSocketFactory),
          name = SocketType.IOPub.toString
        )

        val ioPubClient = system.actorOf(
          Props(classOf[IOPubClient], mockSocketFactory),
          name = SocketType.IOPubClient.toString
        )

        // Give some buffer for the server socket to be bound
        Thread.sleep(100)

        // Register ourselves as sender in IOPubClient's sender map
        val id = UUID.randomUUID().toString
        ioPubClient ! id

        // construct the message and send it
        val result = ExecuteResult(1, Data(), Metadata())
        val header = Header(
          id, "spark",
          UUID.randomUUID().toString, MessageType.ExecuteResult.toString,
          "5.0"
        )
        val kernelMessage = new KernelMessage(
          Seq[String](), "",
          header, HeaderBuilder.empty,
          Metadata(), Json.toJson(result).toString()
        )

        // Send the message on the IOPub server socket
        ioPub ! kernelMessage
        val message = iopubProbe.receiveOne(2000.seconds)
        iopubProbe.forward(ioPubClient, message)

        // ioPubClient should have received the message
        expectMsg(result)
      }
    }
  }
}
