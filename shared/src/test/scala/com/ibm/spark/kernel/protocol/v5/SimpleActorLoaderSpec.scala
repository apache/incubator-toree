package com.ibm.spark.kernel.protocol.v5

import akka.actor.{ActorSelection, Props, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{FunSpecLike, FunSpec, Matchers}
import test.utils.TestProbeProxyActor

class SimpleActorLoaderSpec extends TestKit(ActorSystem("SimpleActorLoaderSpecSystem"))
  with FunSpecLike with Matchers
{
  describe("SimpleActorLoader") {
    //val system = ActorSystem("SimpleActorLoaderSystem")
    val testMessage: String = "Hello Message"

    describe("#load( MessageType )") {
      it("should load a MessageType Actor"){
        //  Create a new test probe to verify our selection works
        val messageTypeProbe: TestProbe = new TestProbe(system)

        //  Add an actor to the system to send a message to
        system.actorOf(
          Props(classOf[TestProbeProxyActor], messageTypeProbe),
          name = MessageType.ExecuteInput.toString
        )

        //  Create the ActorLoader with our test system
        val actorLoader: SimpleActorLoader = SimpleActorLoader(system)

        //  Get the actor and send it a message
        val loadedMessageActor: ActorSelection = actorLoader.load(MessageType.ExecuteInput)

        loadedMessageActor ! testMessage

        //  Assert the probe received the message
        messageTypeProbe.expectMsg(testMessage)
      }
    }

  }
}

