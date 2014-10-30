package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.zeromq.ZMQMessage
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpecLike}
import org.mockito.Matchers._
import org.mockito.Mockito._

class HeartbeatClientSpec extends TestKit(ActorSystem("HeartbeatActorSpec"))
  with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {

  describe("HeartbeatClientActor") {
    val socketFactory = mock[ClientSocketFactory]
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
