package com.ibm.spark.kernel.protocol.v5.dispatch

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.KernelStatus
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}
import play.api.libs.json.Json
import scala.concurrent.duration._

class StatusDispatchSpec extends TestKit(ActorSystem("StatusDispatchSystem"))
with FunSpecLike with Matchers with MockitoSugar {

  describe("StatusDispatch") {
    //  Mock the relay with a probe
    val relayProbe = TestProbe()
    //  Mock the ActorLoader
    val mockActorLoader: ActorLoader = mock[ActorLoader]
    when(mockActorLoader.load(SystemActorType.Relay))
      .thenReturn(system.actorSelection(relayProbe.ref.path.toString))

    val statusDispatchRef: ActorRef = system.actorOf(Props(classOf[StatusDispatch],mockActorLoader))

    describe("#receive( KernelStatusType )") {
      statusDispatchRef ! KernelStatusType.Busy

      it("should send a status message to the relay") {
        //  Check the kernel message is the correct type
        val statusMessage: KernelMessage = relayProbe.receiveOne(500.milliseconds).asInstanceOf[KernelMessage]
        statusMessage.header.msg_type should be (MessageType.Status.toString)
        //  Check the status is what we sent
        val status: KernelStatus = Json.parse(statusMessage.contentString).as[KernelStatus]
         status.execution_state should be (KernelStatusType.Busy.toString)
      }
    }
  }
}
