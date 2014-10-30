package com.ibm.spark.kernel.protocol.v5.socket

import java.nio.charset.Charset

import akka.actor.{ActorSelection, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.ByteString
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5Test._
import com.ibm.spark.kernel.protocol.v5.Utilities._
import com.typesafe.config.ConfigFactory
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}

import scala.concurrent.duration._

object ShellSpec {
  val config ="""
    akka {
      loglevel = "WARNING"
    }"""
}

class ShellSpec extends TestKit(ActorSystem("ShellActorSpec", ConfigFactory.parseString(ShellSpec.config)))
  with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {

  describe("Shell") {
    val socketFactory = mock[ServerSocketFactory]
    val actorLoader = mock[ActorLoader]
    val socketProbe : TestProbe = TestProbe()
    when(socketFactory.Shell(any(classOf[ActorSystem]), any(classOf[ActorRef]))).thenReturn(socketProbe.ref)

    val relayProbe : TestProbe = TestProbe()
    val relaySelection : ActorSelection = system.actorSelection(relayProbe.ref.path)
    when(actorLoader.load(SystemActorType.KernelMessageRelay)).thenReturn(relaySelection)

    val shell = system.actorOf(Props(classOf[Shell], socketFactory, actorLoader))

    describe("#receive") {
      it("( KernelMessage ) should reply with a ZMQMessage via the socket") {
        //  Use the implicit to convert the KernelMessage to ZMQMessage
        val MockZMQMessage : ZMQMessage = MockKernelMessage

        shell ! MockKernelMessage
        socketProbe.expectMsg(MockZMQMessage)
      }

      it("( ZMQMessage ) should forward ZMQ Strings and KernelMessage to Relay") {
        //  Use the implicit to convert the KernelMessage to ZMQMessage
        val MockZMQMessage : ZMQMessage = MockKernelMessage

        shell ! MockZMQMessage

        // Should get the last four (assuming no buffer) strings in UTF-8
        val zmqStrings = MockZMQMessage.frames.map((byteString: ByteString) =>
          new String(byteString.toArray, Charset.forName("UTF-8"))
        ).takeRight(4)

        val kernelMessage: KernelMessage = MockZMQMessage

        relayProbe.expectMsg((zmqStrings, kernelMessage))
      }
    }
  }
}
