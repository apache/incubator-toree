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

package org.apache.toree.kernel.protocol.v5.kernel.socket

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.apache.toree.communication.ZMQMessage
import org.apache.toree.kernel.protocol.v5.kernel.Utilities
import org.apache.toree.kernel.protocol.v5Test._
import Utilities._
import com.typesafe.config.ConfigFactory
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}
import test.utils.MaxAkkaTestTimeout

object IOPubSpec {
  val config ="""
    akka {
      loglevel = "WARNING"
    }"""
}

class IOPubSpec extends TestKit(
  ActorSystem("IOPubActorSpec",
    ConfigFactory.parseString(IOPubSpec.config),
    org.apache.toree.Main.getClass.getClassLoader
  ))
with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {

  describe("IOPubActor") {
    val socketFactory = mock[SocketFactory]
    val probe : TestProbe = TestProbe()
    when(socketFactory.IOPub(any(classOf[ActorSystem]))).thenReturn(probe.ref)

    val socket = system.actorOf(Props(classOf[IOPub], socketFactory))

    // TODO test that the response type changed
    describe("#receive") {
      it("should reply with a ZMQMessage") {
        //  Use the implicit to convert the KernelMessage to ZMQMessage
        val MockZMQMessage : ZMQMessage = MockKernelMessage

        socket ! MockKernelMessage
        probe.expectMsg(MaxAkkaTestTimeout, MockZMQMessage)
      }
    }
  }
}
