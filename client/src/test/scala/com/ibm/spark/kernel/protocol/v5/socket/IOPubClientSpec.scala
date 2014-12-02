package com.ibm.spark.kernel.protocol.v5.socket

import java.util.UUID
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.util.Timeout
import com.ibm.spark.kernel.protocol.v5.client.execution.{DeferredExecution, DeferredExecutionManager}
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.{Promise, Future}
import scala.concurrent.duration._
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.{StreamContent}
import com.ibm.spark.kernel.protocol.v5.Utilities._
import com.typesafe.config.ConfigFactory
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpecLike}
import org.mockito.Mockito._
import org.mockito.Matchers._
import play.api.libs.json.Json
import akka.pattern.ask
import scala.util.Failure

object IOPubClientSpec {
  val config ="""
    akka {
      loglevel = "WARNING"
    }"""
}

class IOPubClientSpec extends TestKit(ActorSystem(
  "IOPubClientSpecSystem", ConfigFactory.parseString(IOPubClientSpec.config)
)) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar with ScalaFutures
{
  implicit val timeout = Timeout(10.seconds)
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
      it("should call a registered callback on stream message") {
        // Generate an id for the kernel message
        val id = UUID.randomUUID().toString

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
        val promise: Promise[String] = Promise()
        val de: DeferredExecution = DeferredExecution().onStream(
          (content: StreamContent) => {
            promise.success(content.text)
          }
        )
        DeferredExecutionManager.add(id, de)
        // Send the message to the IOPubClient
        val zmqMessage: ZMQMessage = kernelMessage

        ioPubClient ! zmqMessage

        whenReady(promise.future) {
          case res: String =>
            res should be eq("bar")
          case _ =>
            fail(s"Received failure when asking IOPubClient")
        }
      }

      it("should not invoke callback when stream message's parent header is null") {
        // Generate an id for the kernel message
        val id = UUID.randomUUID().toString

        // Construct the kernel message
        val result = StreamContent("foo", "bar")
        val header = Header(id, "spark", id, MessageType.Stream.toString, "5.0")

        val kernelMessage = new KernelMessage(
          Seq[String](),
          "",
          header,
          null,
          Metadata(),
          Json.toJson(result).toString()
        )

        // Send the message to the IOPubClient
        val zmqMessage: ZMQMessage = kernelMessage
        val futureResult: Future[Any] = ioPubClient ? zmqMessage
        whenReady(futureResult) {
          case result: Failure[Any] =>
            //  Getting the value of the failure will cause the underlying exception will be thrown
            try {
              result.get
            } catch {
              case t:RuntimeException =>
                t.getMessage should be("Parent Header was null in Kernel Message.")
            }
          case result =>
            fail(s"Did not receive failure!! ${result}")
        }
      }
    }
  }
}
