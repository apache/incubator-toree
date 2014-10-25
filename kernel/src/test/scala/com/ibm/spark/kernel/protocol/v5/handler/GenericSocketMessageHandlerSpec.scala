package com.ibm.spark.kernel.protocol.v5.handler

import akka.actor.{ActorSystem, Props, ActorRef, ActorSelection}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5Test._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpecLike, FunSpec}

class GenericSocketMessageHandlerSpec extends TestKit(ActorSystem("GenericSocketMessageHandlerSystem"))
with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {
  describe("GenericSocketMessageHandler( ActorLoader, SocketType )") {
    //  Create a mock ActorLoader for the Relay we are going to test
    val actorLoader: ActorLoader = mock[ActorLoader]

    //  Create a probe for the ActorSelection that the ActorLoader will return
    val selectionProbe: TestProbe = TestProbe()
    val selection: ActorSelection = system.actorSelection(selectionProbe.ref.path.toString)
    when(actorLoader.load(SocketType.Control)).thenReturn(selection)

    //  The Relay we are going to be testing against
    val genericHandler: ActorRef = system.actorOf(
      Props(classOf[GenericSocketMessageHandler], actorLoader, SocketType.Control)
    )

    describe("#receive( KernelMessage )") {
      genericHandler ! MockKernelMessage

      it("should send the message to the selected actor"){
        selectionProbe.expectMsg(MockKernelMessage)
      }
    }
  }
}
