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
import org.apache.toree.communication.security.{Hmac, SignatureProducerActor}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}

object SignatureProducerActorSpecForIntegration {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class SignatureProducerActorSpecForIntegration extends TestKit(
  ActorSystem(
    "SignatureProducerActorSpec",
    ConfigFactory.parseString(SignatureProducerActorSpecForIntegration.config)
  )
) with ImplicitSender with FunSpecLike with Matchers with BeforeAndAfter
{

  private val sigKey = "12345"

  private var signatureProducer: ActorRef = _

  before {
    val hmac = Hmac(sigKey)
    signatureProducer =
      system.actorOf(Props(classOf[SignatureProducerActor], hmac))

  }

  after {
    signatureProducer = null
  }

  describe("SignatureProducerActor") {
    describe("#receive") {
      it("should return the correct signature for a kernel message") {
        val expectedSignature =
          "1c4859a7606fd93eb5f73c3d9642f9bc860453ba42063961a00d02ed820147b5"
        val message =
          KernelMessage(
            null, "",
            Header("a", "b", "c", "d", "e"),
            ParentHeader("f", "g", "h", "i", "j"),
            Metadata(),
            "<STRING>"
          )

        signatureProducer ! message
        expectMsg(expectedSignature)
      }
    }
  }
}
