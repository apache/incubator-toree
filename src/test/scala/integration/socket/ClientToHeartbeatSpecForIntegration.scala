package integration.socket

import java.io.File

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5.SocketType
import com.ibm.spark.kernel.protocol.v5.socket._
import com.typesafe.config.ConfigFactory
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}
import scala.concurrent.duration._


class ClientToHeartbeatSpecForIntegration extends TestKit(ActorSystem("HeartbeatActorSpec"))
  with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {

  describe("HeartbeatActor") {
    implicit val timeout = Timeout(1.minute)
    val socketFactory = mock[SocketFactory]
    val probe: TestProbe = TestProbe()
    val probeClient: TestProbe = TestProbe()
    when(socketFactory.Heartbeat(any(classOf[ActorSystem]), any(classOf[ActorRef]))).thenReturn(probe.ref)
    when(socketFactory.HeartbeatClient(any(classOf[ActorSystem]), any(classOf[ActorRef]))).thenReturn(probeClient.ref)

    val heartbeat = system.actorOf(Props(classOf[Heartbeat], socketFactory))
    val heartbeatClient = system.actorOf(Props(classOf[HeartbeatClient], socketFactory))

    describe("send heartbeat") {
      it("should send and receive same ZMQMessage") {
        heartbeatClient ? HeartbeatMessage
        probeClient.expectMsgClass(classOf[ZMQMessage])
        probeClient.forward(heartbeat)
        probe.expectMsgClass(classOf[ZMQMessage])
        probe.forward(heartbeatClient)
      }
    }

    describe("send heartbeat") {
      it("should work with real actorsystem and no probes") {
        val system = ActorSystem("iopubtest")

        val socketConfig = SocketConfig.fromConfig(ConfigFactory.parseString(
          """
            {
                "stdin_port": 8000,
                "ip": "127.0.0.1",
                "control_port": 8001,
                "hb_port": 8002,
                "signature_scheme": "hmac-sha256",
                "key": "",
                "shell_port": 8003,
                "transport": "tcp",
                "iopub_port": 8004
            }
          """.stripMargin)
        )
        val socketFactory = new SocketFactory(socketConfig)
        val ioPUB = system.actorOf(Props(classOf[IOPub], socketFactory), name = SocketType.IOPub.toString)
      }
    }
  }
}