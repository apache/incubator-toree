package com.ibm.spark.kernel.protocol.v5

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}
import test.utils.TestProbeProxyActor
import scala.concurrent.duration._

class ActorLoaderSpec extends TestKit(ActorSystem("ActorLoaderSpecSystem"))
with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {
  describe("ActorLoader"){
    describe("#load( MessageType )"){
      it("should load an ActorSelection that has been loaded into the system"){
        val testProbe: TestProbe = TestProbe()
        system.actorOf(Props(classOf[TestProbeProxyActor], testProbe), MessageType.ClearOutput.toString)
        val actorLoader: ActorLoader = SimpleActorLoader(system)
        actorLoader.load(MessageType.ClearOutput) ! "<Test Message>"
        testProbe.expectMsg("<Test Message>")
      }

      it("should expect no message when there is no actor"){
        val testProbe: TestProbe = TestProbe()
        val actorLoader: ActorLoader = SimpleActorLoader(system)
        actorLoader.load(MessageType.CompleteReply) ! "<Test Message>"
        testProbe.expectNoMsg(25.millis)
        // This is to test to see if there the messages go to the actor inbox or the dead mail inbox
        system.actorOf(Props(classOf[TestProbeProxyActor], testProbe), MessageType.CompleteReply.toString)
        testProbe.expectNoMsg(25.millis)
      }
    }
    describe("#load( SocketType )"){
      it("should load an ActorSelection that has been loaded into the system"){
        val testProbe: TestProbe = TestProbe()
        system.actorOf(Props(classOf[TestProbeProxyActor], testProbe), SocketType.Shell.toString)
        val actorLoader: ActorLoader = SimpleActorLoader(system)
        actorLoader.load(SocketType.Shell) ! "<Test Message>"
        testProbe.expectMsg("<Test Message>")
      }

      it("should expect no message when there is no actor"){
        val testProbe: TestProbe = TestProbe()
        val actorLoader: ActorLoader = SimpleActorLoader(system)
        actorLoader.load(SocketType.IOPub) ! "<Test Message>"
        testProbe.expectNoMsg(25.millis)
        // This is to test to see if there the messages go to the actor inbox or the dead mail inbox
        system.actorOf(Props(classOf[TestProbeProxyActor], testProbe), SocketType.IOPub.toString)
        testProbe.expectNoMsg(25.millis)
      }

    }
  }
}
