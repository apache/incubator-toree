package com.ibm.spark.client

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.client.SparkKernelClient
import com.ibm.spark.kernel.protocol.v5.client.execution.ExecuteRequestTuple
import com.ibm.spark.kernel.protocol.v5.content.ExecuteRequest
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}

class SparkKernelClientSpec extends TestKit(ActorSystem("RelayActorSystem"))
  with Matchers with MockitoSugar with FunSpecLike {

  val actorLoader = mock[ActorLoader]
  val client = new SparkKernelClient(actorLoader, system)
  val probe = TestProbe()
  when(actorLoader.load(MessageType.ExecuteRequest))
    .thenReturn(system.actorSelection(probe.ref.path.toString))

  describe("SparkKernelClient") {
    describe("#submit") {
      it("should send an ExecuteRequest message") {
        client.execute("val foo = 2")
        probe.expectMsgClass(classOf[ExecuteRequest])
      }
    }

    describe("#stream") {
      it("should send an ExecuteRequest message") {
        val func = (x: Any) => println(x)
        client.execute("val foo = 2")
        probe.expectMsgClass(classOf[ExecuteRequestTuple])
      }
    }
  }
}
