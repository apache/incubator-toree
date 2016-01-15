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
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.communication.security.{Hmac, SignatureCheckerActor}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}
import play.api.libs.json.Json

object SignatureCheckerActorSpecForIntegration {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class SignatureCheckerActorSpecForIntegration extends TestKit(
  ActorSystem(
    "SignatureCheckerActorSpec",
    ConfigFactory.parseString(SignatureCheckerActorSpecForIntegration.config)
  )
) with ImplicitSender with FunSpecLike with Matchers with BeforeAndAfter
{

  private val sigKey = "12345"
  private val signature =
    "1c4859a7606fd93eb5f73c3d9642f9bc860453ba42063961a00d02ed820147b5"
  private val goodMessage =
    KernelMessage(
      null, signature,
      Header("a", "b", "c", "d", "e"),
      ParentHeader("f", "g", "h", "i", "j"),
      Metadata(),
      "<STRING>"
    )
  private val badMessage =
    KernelMessage(
      null, "wrong signature",
      Header("a", "b", "c", "d", "e"),
      ParentHeader("f", "g", "h", "i", "j"),
      Metadata(),
      "<STRING>"
    )

  private var signatureChecker: ActorRef = _

  before {
    val hmac = Hmac(sigKey)
    signatureChecker =
      system.actorOf(Props(classOf[SignatureCheckerActor], hmac))
  }

  after {
    signatureChecker = null
  }

  describe("SignatureCheckerActor") {
    describe("#receive") {
      it("should return true if the kernel message is valid") {
        val blob =
          Json.stringify(Json.toJson(goodMessage.header)) ::
          Json.stringify(Json.toJson(goodMessage.parentHeader)) ::
          Json.stringify(Json.toJson(goodMessage.metadata)) ::
          goodMessage.contentString ::
          Nil
        signatureChecker ! ((goodMessage.signature, blob))
        expectMsg(true)
      }

      it("should return false if the kernel message is invalid") {
        val blob =
          Json.stringify(Json.toJson(badMessage.header)) ::
          Json.stringify(Json.toJson(badMessage.parentHeader)) ::
          Json.stringify(Json.toJson(badMessage.metadata)) ::
          badMessage.contentString ::
          Nil
        signatureChecker ! ((badMessage.signature, blob))
        expectMsg(false)
      }
    }
  }
}
