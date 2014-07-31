package com.ibm.spark.kernel.protocol.v5

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5.MessageType.MessageType
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}

class RelaySpec extends TestKit(ActorSystem("RelayActorSystem"))
with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {
  //  The header for the message
  val header : Header = Header("<UUID>","<USER>","<SESSION>",
    MessageType.ClearOutput.toString, "<VERSION>")
  //  The parent header for the message
  val parentHeader: Header = Header("<PARENT-UUID>","<PARENT-USER>","<PARENT-SESSION>",
    MessageType.ClearOutput.toString, "<PARENT-VERSION>")
  //  The actual kernel message
  val kernelMessage : KernelMessage = KernelMessage(Seq("<ID>"), "<SIGNATURE>", header,
    parentHeader, Metadata(), "<CONTENT>")
  //  Use the implicit to convert the KernelMessage to ZMQMessage
  val zmqMessage : ZMQMessage = kernelMessage

  describe("Relay"){
    describe("#receive"){
    	it("should relay KernelMessage"){
        val actorLoader : ActorLoader = mock[ActorLoader]
        val probe : TestProbe = TestProbe()
        when(actorLoader.load(any[MessageType])).thenReturn(probe.ref)
        val relay : ActorRef = system.actorOf(Props(classOf[Relay], actorLoader))
        relay ! kernelMessage
        probe.expectMsg(kernelMessage)
    	}

      it("should relay ZMQMessage as KernelMessage"){
        val actorLoader : ActorLoader = mock[ActorLoader]
        val probe : TestProbe = TestProbe()
        when(actorLoader.load(any[MessageType])).thenReturn(probe.ref)
        val relay : ActorRef = system.actorOf(Props(classOf[Relay], actorLoader))
        relay ! zmqMessage
        probe.expectMsg(kernelMessage)
      }
    }
  }

}
