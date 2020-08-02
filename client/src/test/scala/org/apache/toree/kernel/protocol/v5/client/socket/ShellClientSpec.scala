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

package org.apache.toree.kernel.protocol.v5.client.socket

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import org.apache.toree.communication.ZMQMessage
import org.apache.toree.communication.security.SecurityActorType
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.client.ActorLoader
import org.apache.toree.kernel.protocol.v5.content.ExecuteRequest
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{Matchers, FunSpecLike}
import org.mockito.Mockito._
import org.mockito.Matchers._
import play.api.libs.json.Json

class ShellClientSpec extends TestKit(ActorSystem("ShellActorSpec"))
  with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {
  private val SignatureEnabled = true

  describe("ShellClientActor") {
    val socketFactory = mock[SocketFactory]
    val mockActorLoader = mock[ActorLoader]
    val probe : TestProbe = TestProbe()
    when(socketFactory.ShellClient(
      any(classOf[ActorSystem]), any(classOf[ActorRef])
    )).thenReturn(probe.ref)

    val signatureManagerProbe = TestProbe()
    doReturn(system.actorSelection(signatureManagerProbe.ref.path.toString))
      .when(mockActorLoader).load(SecurityActorType.SignatureManager)

    val shellClient = system.actorOf(Props(
      classOf[ShellClient], socketFactory, mockActorLoader, SignatureEnabled
    ))

    describe("send execute request") {
      it("should send execute request") {
        val request = ExecuteRequest(
          "foo", false, true, UserExpressions(), true
        )
        val header = Header(
          UUID.randomUUID().toString, "spark",
          UUID.randomUUID().toString, MessageType.Incoming.ExecuteRequest.toString,
          "5.0"
        )
        val kernelMessage = KernelMessage(
          Seq[Array[Byte]](), "",
          header, HeaderBuilder.empty,
          Metadata(), Json.toJson(request).toString
        )
        shellClient ! kernelMessage

        // Echo back the kernel message sent to have a signature injected
        signatureManagerProbe.expectMsgClass(classOf[KernelMessage])
        signatureManagerProbe.reply(kernelMessage)

        probe.expectMsgClass(classOf[ZMQMessage])
      }
    }
  }
}
