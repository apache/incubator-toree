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

package org.apache.toree.kernel.protocol.v5.handler

import akka.actor.{ActorSystem, Props, ActorRef, ActorSelection}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.kernel.protocol.v5Test._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{Matchers, FunSpecLike}
import test.utils.MaxAkkaTestTimeout

class GenericSocketMessageHandlerSpec extends TestKit(
  ActorSystem(
    "GenericSocketMessageHandlerSystem",
    None,
    Some(org.apache.toree.Main.getClass.getClassLoader)
  ))
with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {
  describe("GenericSocketMessageHandler( ActorLoader, SocketType )") {
    //  Create a mock ActorLoader for the Relay we are going to test
    val actorLoader: ActorLoader = mock[ActorLoader]

    //  Create a probe for the ActorSelection that the ActorLoader will return
    val selectionProbe: TestProbe = TestProbe()
    val selection: ActorSelection = system.actorSelection(selectionProbe.ref.path.toString)
    when(actorLoader.load(SocketType.Control)).thenReturn(selection)

    //  The Relay we are going to be testing against
    val genericHandler: ActorRef = system.actorOf(
      Props(classOf[GenericSocketMessageHandler], actorLoader, SocketType.Control)
    )

    describe("#receive( KernelMessage )") {
      genericHandler ! MockKernelMessage

      it("should send the message to the selected actor"){
        selectionProbe.expectMsg(MaxAkkaTestTimeout, MockKernelMessage)
      }
    }
  }
}
