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

import java.nio.charset.Charset

import akka.actor.{Props, ActorSelection, ActorRef, ActorSystem}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.util.ByteString
import org.apache.toree.communication.ZMQMessage
import org.apache.toree.kernel.protocol.v5.kernel.Utilities._
import org.apache.toree.kernel.protocol.v5Test._
import org.apache.toree.kernel.protocol.v5.{KernelMessage, SystemActorType}
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import com.typesafe.config.ConfigFactory
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{Matchers, FunSpecLike}
import org.mockito.Mockito._
import org.mockito.Matchers._
import test.utils.MaxAkkaTestTimeout

object StdinSpec {
  val config ="""
    akka {
      loglevel = "WARNING"
    }"""
}

class StdinSpec extends TestKit(ActorSystem(
  "StdinActorSpec",
  ConfigFactory.parseString(StdinSpec.config),
  org.apache.toree.Main.getClass.getClassLoader
)) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {
  describe("Stdin") {
    val socketFactory = mock[SocketFactory]
    val actorLoader = mock[ActorLoader]
    val socketProbe : TestProbe = TestProbe()
    when(socketFactory.Stdin(any(classOf[ActorSystem]), any(classOf[ActorRef]))).thenReturn(socketProbe.ref)

    val relayProbe : TestProbe = TestProbe()
    val relaySelection : ActorSelection = system.actorSelection(relayProbe.ref.path)
    when(actorLoader.load(SystemActorType.KernelMessageRelay)).thenReturn(relaySelection)

    val stdin = system.actorOf(Props(classOf[Stdin], socketFactory, actorLoader))

    describe("#receive") {
      it("( KernelMessage ) should reply with a ZMQMessage via the socket") {
        //  Use the implicit to convert the KernelMessage to ZMQMessage
        val MockZMQMessage : ZMQMessage = MockKernelMessage

        stdin ! MockKernelMessage
        socketProbe.expectMsg(MockZMQMessage)
      }

      it("( ZMQMessage ) should forward ZMQ Strings and KernelMessage to Relay") {
        //  Use the implicit to convert the KernelMessage to ZMQMessage
        val MockZMQMessage : ZMQMessage = MockKernelMessage

        stdin ! MockZMQMessage

        // Should get the last four (assuming no buffer) strings in UTF-8
        val zmqStrings = MockZMQMessage.frames.map((byteString: ByteString) =>
          new String(byteString.toArray, Charset.forName("UTF-8"))
        ).takeRight(4)

        val kernelMessage: KernelMessage = MockZMQMessage

        relayProbe.expectMsg(MaxAkkaTestTimeout, (zmqStrings, kernelMessage))
      }
    }
  }
}

