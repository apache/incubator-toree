package com.ibm.spark.kernel.protocol.v5

import akka.actor.Actor.Receive
import akka.actor.{ActorSelection, Props, Actor, ActorSystem}
import akka.testkit.TestProbe
import com.ibm.spark.kernel.protocol.v5.handler.GenericSocketMessageHandler
import org.scalatest.{Matchers, FunSpec}
import test.utils.TestProbeProxyActor

class SimpleActorLoaderSpec extends FunSpec with Matchers {
  describe("SimpleActorLoader") {
    val actorSystem = ActorSystem("SimpleActorLoaderSystem")
    val testMessage: String = "Hello Message"

    describe("#load( MessageType )") {
      it("should load a MessageType Actor"){
        //  Create a new test probe to verify our selection works
        val messageTypeProbe: TestProbe = new TestProbe(actorSystem)

        //  Add an actor to the system to send a message to
        actorSystem.actorOf(
          Props(classOf[TestProbeProxyActor], messageTypeProbe),
          name = MessageType.ExecuteInput.toString
        )

        //  Create the ActorLoader with our test system
        val actorLoader: SimpleActorLoader = SimpleActorLoader(actorSystem)

        //  Get the actor and send it a message
        val loadedMessageActor: ActorSelection = actorLoader.load(MessageType.ExecuteInput)

        loadedMessageActor ! testMessage

        //  Assert the probe received the message
        messageTypeProbe.expectMsg(testMessage)
      }
    }

  }
}

