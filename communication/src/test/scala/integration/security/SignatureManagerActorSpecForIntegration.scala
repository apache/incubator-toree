/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package integration.security

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.apache.toree.communication.security.SignatureManagerActor
import org.apache.toree.kernel.protocol.v5.{KernelMessage, _}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}
import play.api.libs.json.Json

import scala.concurrent.duration._

object SignatureManagerActorSpecForIntegration {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class SignatureManagerActorSpecForIntegration extends TestKit(
  ActorSystem(
    "SignatureManagerActorSpec",
    ConfigFactory.parseString(SignatureManagerActorSpecForIntegration.config)
  )
) with ImplicitSender with FunSpecLike with Matchers with BeforeAndAfter
{
  private val IncomingMessageType = "d" // Needed for valid signature
  
  private val sigKey = "12345"
  private val signature =
    "1c4859a7606fd93eb5f73c3d9642f9bc860453ba42063961a00d02ed820147b5"
  private val goodIncomingMessage =
    KernelMessage(
      List(), signature,
      Header("a", "b", "c", IncomingMessageType, "e"),
      ParentHeader("f", "g", "h", "i", "j"),
      Metadata(),
      "<STRING>"
    )
  private val badIncomingMessage =
    KernelMessage(
      List(), "wrong signature",
      Header("a", "b", "c", IncomingMessageType, "e"),
      ParentHeader("f", "g", "h", "i", "j"),
      Metadata(),
      "<STRING>"
    )

  private var signatureManager: ActorRef = _
  private var signatureManagerWithNoIncoming: ActorRef = _

  before {
    signatureManager =
      system.actorOf(Props(
        classOf[SignatureManagerActor], sigKey
      ))

    signatureManagerWithNoIncoming =
      system.actorOf(Props(
        classOf[SignatureManagerActor], sigKey
      ))
  }

  after {
    signatureManager = null
  }

  describe("SignatureManagerActor") {
    describe("#receive") {
      describe("when receiving an incoming message") {
        it("should return true if the signature is valid") {
          val blob =
            Json.stringify(Json.toJson(goodIncomingMessage.header)) ::
            Json.stringify(Json.toJson(goodIncomingMessage.parentHeader)) ::
            Json.stringify(Json.toJson(goodIncomingMessage.metadata)) ::
            goodIncomingMessage.contentString ::
            Nil
          signatureManager ! ((goodIncomingMessage.signature, blob))
          expectMsg(true)
        }

        it("should return false if the signature is invalid") {
          val blob =
            Json.stringify(Json.toJson(badIncomingMessage.header)) ::
            Json.stringify(Json.toJson(badIncomingMessage.parentHeader)) ::
            Json.stringify(Json.toJson(badIncomingMessage.metadata)) ::
            badIncomingMessage.contentString ::
            Nil
          signatureManager ! ((badIncomingMessage.signature, blob))
          expectMsg(false)
        }
      }

      describe("when receiving an outgoing message") {
        it("should insert a valid signature into the message and return it") {
          // Sending to signature manager that has no incoming messages
          signatureManagerWithNoIncoming ! badIncomingMessage

          val newKernelMessage =
            receiveOne(5.seconds).asInstanceOf[KernelMessage]

          newKernelMessage.signature should be (signature)
        }
      }
    }
  }
}
