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



/*
package integration.socket

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.zeromq._
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content.ExecuteRequest
import org.apache.toree.kernel.protocol.v5.socket._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}
import play.api.libs.json.Json

class ClientToShellSpecForIntegration extends TestKit(ActorSystem("ShellActorSpec"))
with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {
  describe("ShellActor") {
    val clientSocketFactory = mock[ClientSocketFactory]
    val serverSocketFactory = mock[ServerSocketFactory]
    val actorLoader = mock[ActorLoader]
    val probe : TestProbe = TestProbe()
    val probeClient : TestProbe = TestProbe()
    when(serverSocketFactory.Shell(
      any(classOf[ActorSystem]), any(classOf[ActorRef])
    )).thenReturn(probe.ref)
    when(clientSocketFactory.ShellClient(
      any(classOf[ActorSystem]), any(classOf[ActorRef])
    )).thenReturn(probeClient.ref)
    when(actorLoader.load(SystemActorType.KernelMessageRelay))
      .thenReturn(system.actorSelection(probe.ref.path.toString))

    val shell = system.actorOf(Props(
      classOf[Shell], serverSocketFactory, actorLoader
    ))
    val shellClient = system.actorOf(Props(
      classOf[ShellClient], clientSocketFactory
    ))

    val request = ExecuteRequest(
      """val x = "foo"""", false, true, UserExpressions(), true
    )
    val header = Header(
      UUID.randomUUID().toString, "spark",
      UUID.randomUUID().toString, MessageType.ExecuteRequest.toString,
      "5.0"
    )
    val kernelMessage = KernelMessage(
      Seq[String](), "", header, HeaderBuilder.empty, Metadata(),
      Json.toJson(request).toString
    )

    describe("send execute request") {
      it("should send execute request") {
        shellClient ! kernelMessage
        probeClient.expectMsgClass(classOf[ZMQMessage])
        probeClient.forward(shell)
        probe.expectMsgClass(classOf[Tuple2[Seq[_], KernelMessage]])
        probe.forward(shellClient)
      }
    }
  }
}
*/