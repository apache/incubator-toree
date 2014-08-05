package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.zeromq.ZMQMessage
import com.typesafe.config.ConfigFactory
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}

object HeartbeatClientSpec {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class HeartbeatClientSpec extends TestKit(ActorSystem("HeartbeatActorSpec", ConfigFactory.parseString(HeartbeatSpec.config)))
with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {

  describe("HeartbeatClientActor") {
    val socketFactory = mock[SocketFactory]
    val probe : TestProbe = TestProbe()
    when(socketFactory.HeartbeatClient(any(classOf[ActorSystem]), any(classOf[ActorRef]))).thenReturn(probe.ref)

    val heartbeatClient = system.actorOf(Props(classOf[HeartbeatClient], socketFactory))

    describe("send heartbeat") {
      it("should send ping ZMQMessage") {
        heartbeatClient ! HeartbeatMessage
        probe.expectMsgClass(classOf[ZMQMessage])
      }
    }
  }
}
