package com.ibm.spark.kernel.protocol.v5.handler

import akka.actor.{ActorSelection, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.ibm.spark.client.ExecuteHandler
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.ExecuteRequest
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}

class ExecuteHandlerSpec extends TestKit(ActorSystem("ExecuteHandlerSpec"))
  with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {

  describe("ExecuteHandlerActor") {
    val actorLoader = mock[ActorLoader]
    val probe : TestProbe = TestProbe()
    val selection : ActorSelection = system.actorSelection(probe.ref.path)
    when(actorLoader.load(SocketType.ShellClient)).thenReturn(selection)

    val executeHandler = system.actorOf(Props(classOf[ExecuteHandler], actorLoader))

    describe("send execute request") {
      it("should send execute request as kernel message") {
        val request = ExecuteRequest("foo", false, true, UserExpressions(), true)
        executeHandler ! request
        probe.expectMsgClass(classOf[KernelMessage])
      }
    }
  }
}