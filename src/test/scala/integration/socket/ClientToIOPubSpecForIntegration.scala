package integration.socket

import java.io.File
import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestProbe
import com.ibm.spark.client.CallbackMap
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.ExecuteResult
import com.ibm.spark.kernel.protocol.v5.socket._
import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json.Json

class ClientToIOPubSpecForIntegration extends FunSpec with Matchers {
  describe("Client-IOPub Integration"){
    describe("Client"){
      it("should connect to IOPub Socket"){
        // setup
        val system = ActorSystem("iopubtest")
        val profile = Option(new File("src/test/resources/kernel-profiles/IOPubIntegrationProfile.json"))
        val socketConfigReader = new SocketConfigReader(profile)
        val socketFactory = new SocketFactory(socketConfigReader.getSocketConfig)
        //  Server and client sockets
        val ioPub = system.actorOf(Props(classOf[IOPub], socketFactory),
          name = SocketType.IOPub.toString)

        val ioPubClient = system.actorOf(Props(classOf[IOPubClient], socketFactory),
            name = SocketType.IOPubClient.toString)

        //  Give some buffer for the server socket to be bound
        Thread.sleep(500)

        // register a callback to call so we can assert against the message
        val id = UUID.randomUUID().toString
        val executeResultProbe: TestProbe = new TestProbe(system)
        CallbackMap.put(id, (x: ExecuteResult) => executeResultProbe.ref.tell(x, executeResultProbe.ref))

        // construct the message and send it
        val result = ExecuteResult(1, Data(), Metadata())
        val header = Header(id, "spark", UUID.randomUUID().toString, MessageType.ExecuteResult.toString, "5.0")
        val kernelMessage = new KernelMessage(Seq[String](), "", header, EmptyHeader, Metadata(), Json.toJson(result).toString())

        //  Send the message on the IOPub server socket
        ioPub ! kernelMessage

        //  Wait for the message to bubble back
        executeResultProbe.expectMsg(result)

        system.shutdown()
      }
    }
  }
}
