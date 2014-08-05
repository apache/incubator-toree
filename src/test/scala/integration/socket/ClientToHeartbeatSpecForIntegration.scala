package integration.socket

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5.socket.{Heartbeat, HeartbeatClient, HeartbeatMessage, SocketFactory}
import com.typesafe.config.ConfigFactory
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}

object ClientToHeartbeatSpecForIntegration {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class ClientToHeartbeatSpecForIntegration
  extends TestKit(ActorSystem("HeartbeatActorSpec", ConfigFactory.parseString(ClientToHeartbeatSpecForIntegration.config)))
  with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {

  describe("HeartbeatActor") {
    val socketFactory = mock[SocketFactory]
    val probe : TestProbe = TestProbe()
    val probeClient : TestProbe = TestProbe()
    when(socketFactory.Heartbeat(any(classOf[ActorSystem]), any(classOf[ActorRef]))).thenReturn(probe.ref)
    when(socketFactory.HeartbeatClient(any(classOf[ActorSystem]), any(classOf[ActorRef]))).thenReturn(probeClient.ref)

    val heartbeat = system.actorOf(Props(classOf[Heartbeat], socketFactory))
    val heartbeatClient = system.actorOf(Props(classOf[HeartbeatClient], socketFactory))

    describe("send heartbeat") {
      it("should send and receive same ZMQMessage") {
        heartbeatClient ! HeartbeatMessage
        probeClient.expectMsgClass(classOf[ZMQMessage])
        probeClient.forward(heartbeat)
        probe.expectMsgClass(classOf[ZMQMessage])
        probe.forward(heartbeatClient)
      }
    }
  }
}