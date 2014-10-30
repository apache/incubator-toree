package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5Test._
import com.ibm.spark.kernel.protocol.v5.Utilities._
import com.typesafe.config.ConfigFactory
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}

object IOPubSpec {
  val config ="""
    akka {
      loglevel = "WARNING"
    }"""
}

class IOPubSpec extends TestKit(ActorSystem("IOPubActorSpec", ConfigFactory.parseString(IOPubSpec.config)))
with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {

  describe("IOPubActor") {
    val socketFactory = mock[ServerSocketFactory]
    val probe : TestProbe = TestProbe()
    when(socketFactory.IOPub(any(classOf[ActorSystem]))).thenReturn(probe.ref)

    val socket = system.actorOf(Props(classOf[IOPub], socketFactory))

    // TODO test that the response type changed
    describe("#receive") {
      it("should reply with a ZMQMessage") {
        //  Use the implicit to convert the KernelMessage to ZMQMessage
        val MockZMQMessage : ZMQMessage = MockKernelMessage

        socket ! MockKernelMessage
        probe.expectMsg(MockZMQMessage)
      }
    }
  }
}
