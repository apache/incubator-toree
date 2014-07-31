package com.ibm.kernel.protocol.v5.socket

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.ByteString
import akka.zeromq.ZMQMessage
import com.typesafe.config.ConfigFactory
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}

object HeartbeatSpec {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class HeartbeatSpec extends TestKit(ActorSystem("HeartbeatActorSpec", ConfigFactory.parseString(HeartbeatSpec.config)))
  with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {
  val SomeMessage: String = "some message"
  val SomeZMQMessage: ZMQMessage = ZMQMessage(ByteString(SomeMessage.getBytes))


  describe("HeartbeatActor") {
    val socketFactory = mock[SocketFactory]
    val probe : TestProbe = TestProbe()
    when(socketFactory.Heartbeat(any(classOf[ActorSystem]), any(classOf[ActorRef]))).thenReturn(probe.ref)

    val heartbeat = system.actorOf(Props(classOf[Heartbeat], socketFactory))


    describe("#receive") {
      it("should reply with the same string") {
        heartbeat ! SomeMessage
        probe.expectMsg(SomeMessage)
      }

      it("should reply with the same ZMQMessage") {
        heartbeat ! SomeZMQMessage
        probe.expectMsg(SomeZMQMessage)
      }
    }
  }
}
