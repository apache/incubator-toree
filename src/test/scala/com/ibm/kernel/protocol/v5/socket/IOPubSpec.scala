package com.ibm.kernel.protocol.v5.socket

import akka.actor.{Props, ActorRef, ActorSystem}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.util.ByteString
import akka.zeromq.ZMQMessage
import com.typesafe.config.ConfigFactory
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpecLike}

object IOPubSpec {
  val config ="""
    akka {
      loglevel = "WARNING"
    }"""
}

class IOPubSpec extends TestKit(ActorSystem("IOPubActorSpec", ConfigFactory.parseString(IOPubSpec.config)))
with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {

  // TODO replace with actual kernel message
  val KernelMessage: String = "some kernel message"

  describe("IOPubActor") {
    val socketFactory = mock[SocketFactory]
    val probe : TestProbe = TestProbe()
    when(socketFactory.IOPub(any(classOf[ActorSystem]))).thenReturn(probe.ref)

    val socket = system.actorOf(Props(classOf[IOPub], socketFactory))

    // TODO test that the response type changed
    describe("#receive") {
      it("should reply with a ZMQMessage") {
        socket ! KernelMessage
        probe.expectMsg(KernelMessage)
      }
    }
  }
}
