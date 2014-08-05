package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.{ActorSelection, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.ibm.spark.kernel.protocol.v5.{SystemActorType, ActorLoader}
import com.ibm.spark.kernel.protocol.v5Test._
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

  describe("ShellActor") {
    val socketFactory = mock[SocketFactory]
    val actorLoader = mock[ActorLoader]
    val socketProbe : TestProbe = TestProbe()
    when(socketFactory.Shell(any(classOf[ActorSystem]), any(classOf[ActorRef]))).thenReturn(socketProbe.ref)

    val relayProbe : TestProbe = TestProbe()
    val relaySelection : ActorSelection = system.actorSelection(relayProbe.ref.path)
    when(actorLoader.load(SystemActorType.Relay)).thenReturn(relaySelection)

    val shell = system.actorOf(Props(classOf[Shell], socketFactory, actorLoader))

    describe("#receive") {
      it("( KernelMessage ) should reply with a ZMQMessage via the socket") {
        shell ! MockKernelMessage
        socketProbe.expectMsg(MockZMQMessage)
      }

      it("( ZMQMessage ) should forward KernelMessage to Relay") {
        shell ! MockZMQMessage
        relayProbe.expectMsg(MockKernelMessage)
      }
    }
  }
}
