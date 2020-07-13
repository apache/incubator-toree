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

package org.apache.toree.kernel.protocol.v5.kernel

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.apache.toree.kernel.protocol.v5.{MessageType, SocketType}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}
import test.utils.TestProbeProxyActor
import test.utils.MaxAkkaTestTimeout

class ActorLoaderSpec extends TestKit(
  ActorSystem(
    "ActorLoaderSpecSystem",
    None,
    Some(org.apache.toree.Main.getClass.getClassLoader)
  ))
with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {
  describe("ActorLoader"){
    describe("#load( MessageType )"){
      it("should load an ActorSelection that has been loaded into the system"){
        val testProbe: TestProbe = TestProbe()
        system.actorOf(Props(classOf[TestProbeProxyActor], testProbe),
          MessageType.Outgoing.ClearOutput.toString)
        val actorLoader: ActorLoader = SimpleActorLoader(system)
        actorLoader.load(MessageType.Outgoing.ClearOutput) ! "<Test Message>"
        testProbe.expectMsg("<Test Message>")
      }

      it("should expect no message when there is no actor"){
        val testProbe: TestProbe = TestProbe()
        val actorLoader: ActorLoader = SimpleActorLoader(system)
        actorLoader.load(MessageType.Outgoing.CompleteReply) ! "<Test Message>"
        testProbe.expectNoMessage(MaxAkkaTestTimeout)
        // This is to test to see if there the messages go to the actor inbox or the dead mail inbox
        system.actorOf(Props(classOf[TestProbeProxyActor], testProbe),
          MessageType.Outgoing.CompleteReply.toString)
        testProbe.expectNoMessage(MaxAkkaTestTimeout)
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
        testProbe.expectNoMessage(MaxAkkaTestTimeout)
        // This is to test to see if there the messages go to the actor inbox or the dead mail inbox
        system.actorOf(Props(classOf[TestProbeProxyActor], testProbe), SocketType.IOPub.toString)
        testProbe.expectNoMessage(MaxAkkaTestTimeout)
      }

    }
  }
}
