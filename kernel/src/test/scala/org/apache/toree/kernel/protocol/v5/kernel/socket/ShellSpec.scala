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

import akka.actor.{ActorSelection, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.ByteString
import org.apache.toree.communication.ZMQMessage
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.kernel.{ActorLoader, Utilities}
import org.apache.toree.kernel.protocol.v5Test._
import Utilities._
import com.typesafe.config.ConfigFactory
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}
import test.utils.MaxAkkaTestTimeout

object ShellSpec {
  val config ="""
    akka {
      loglevel = "WARNING"
    }"""
}

class ShellSpec extends TestKit(
  ActorSystem(
    "ShellActorSpec",
    ConfigFactory.parseString(ShellSpec.config),
    org.apache.toree.Main.getClass.getClassLoader
  ))
  with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {

  describe("Shell") {
    val socketFactory = mock[SocketFactory]
    val actorLoader = mock[ActorLoader]
    val socketProbe : TestProbe = TestProbe()
    when(socketFactory.Shell(any(classOf[ActorSystem]), any(classOf[ActorRef]))).thenReturn(socketProbe.ref)

    val relayProbe : TestProbe = TestProbe()
    val relaySelection : ActorSelection = system.actorSelection(relayProbe.ref.path)
    when(actorLoader.load(SystemActorType.KernelMessageRelay)).thenReturn(relaySelection)

    val shell = system.actorOf(Props(classOf[Shell], socketFactory, actorLoader))

    describe("#receive") {
      it("( KernelMessage ) should reply with a ZMQMessage via the socket") {
        //  Use the implicit to convert the KernelMessage to ZMQMessage
        val MockZMQMessage : ZMQMessage = MockKernelMessage

        shell ! MockKernelMessage
        socketProbe.expectMsg(MockZMQMessage)
      }

      it("( ZMQMessage ) should forward ZMQ Strings and KernelMessage to Relay") {
        //  Use the implicit to convert the KernelMessage to ZMQMessage
        val MockZMQMessage : ZMQMessage = MockKernelMessage

        shell ! MockZMQMessage

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
