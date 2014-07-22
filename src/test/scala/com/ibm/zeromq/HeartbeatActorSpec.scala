package com.ibm.zeromq

import akka.actor.{Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.ByteString
import akka.zeromq.ZMQMessage
import com.typesafe.config.ConfigFactory
import org.scalatest.{FunSpecLike, Matchers}

object HeartbeatActorSpec {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class HeartbeatActorSpec extends TestKit(ActorSystem("HeartbeatActorSpec", ConfigFactory.parseString(HeartbeatActorSpec.config)))
  with ImplicitSender with FunSpecLike with Matchers {
  val SomeMessage: String = "some message"
  val SomeZMQMessage: ZMQMessage = ZMQMessage(ByteString(SomeMessage.getBytes))
  

  describe("HeartbeatActor") {
    val heartbeat = system.actorOf(Props[HeartbeatActor])

    describe("#receive") {
      it("should reply with the same string") {
        heartbeat ! SomeMessage
        expectMsg(SomeMessage)
      }

      it("should reply with the same ZMQMessage") {
        heartbeat ! SomeZMQMessage
        expectMsg(SomeZMQMessage)
      }
    }
  }
}
