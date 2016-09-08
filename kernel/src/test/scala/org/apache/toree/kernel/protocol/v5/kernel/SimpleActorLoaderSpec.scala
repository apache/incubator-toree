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

import akka.actor.{ActorSelection, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import org.apache.toree.kernel.protocol.v5.MessageType
import org.scalatest.{FunSpecLike, Matchers}
import test.utils.TestProbeProxyActor
import test.utils.MaxAkkaTestTimeout

class SimpleActorLoaderSpec extends TestKit(
  ActorSystem(
    "SimpleActorLoaderSpecSystem",
    None,
    Some(org.apache.toree.Main.getClass.getClassLoader)
  )
)
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
          name = MessageType.Outgoing.ExecuteInput.toString
        )

        //  Create the ActorLoader with our test system
        val actorLoader: SimpleActorLoader = SimpleActorLoader(system)

        //  Get the actor and send it a message
        val loadedMessageActor: ActorSelection =
          actorLoader.load(MessageType.Outgoing.ExecuteInput)

        loadedMessageActor ! testMessage

        //  Assert the probe received the message
        messageTypeProbe.expectMsg(MaxAkkaTestTimeout, testMessage)
      }
    }

  }
}

