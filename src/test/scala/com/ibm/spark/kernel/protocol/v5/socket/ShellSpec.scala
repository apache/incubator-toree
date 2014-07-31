package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.ByteString
import akka.zeromq.ZMQMessage
import com.typesafe.config.ConfigFactory
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}

object ShellSpec {
  val config ="""
    akka {
      loglevel = "WARNING"
    }"""
}

class ShellSpec extends TestKit(ActorSystem("ShellActorSpec", ConfigFactory.parseString(ShellSpec.config)))
  with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {
  val SomeMessage: String = "some message"
  val SomeZMQMessage: ZMQMessage = ZMQMessage(ByteString(SomeMessage.getBytes))


  describe("ShellActor") {
    val socketFactory = mock[SocketFactory]
    val probe : TestProbe = TestProbe()
    when(socketFactory.Shell(any(classOf[ActorSystem]), any(classOf[ActorRef]))).thenReturn(probe.ref)

    val shell = system.actorOf(Props(classOf[Shell], socketFactory))

    describe("#receive") {
      it("should reply with the same string") {
        shell ! SomeMessage
        probe.expectMsg(SomeMessage)
      }

      it("should reply with the same ZMQMessage") {
        shell ! SomeZMQMessage
        probe.expectMsg(SomeZMQMessage)
      }
    }
  }
}
