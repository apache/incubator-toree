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
import akka.actor.{ActorSelection, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.apache.toree.Main
import org.apache.toree.kernel.protocol.v5.content.KernelInfoReply
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.kernel.protocol.v5._
import org.mockito.AdditionalMatchers.{not => mockNot}
import org.mockito.Matchers.{eq => mockEq}
import com.typesafe.config.ConfigFactory
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}
import play.api.libs.json.Json
import test.utils.MaxAkkaTestTimeout

object KernelInfoRequestHandlerSpec {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class KernelInfoRequestHandlerSpec extends TestKit(
  ActorSystem("KernelInfoRequestHandlerSpec",
    ConfigFactory.parseString(KernelInfoRequestHandlerSpec.config),
    Main.getClass.getClassLoader)
) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {
  val actorLoader: ActorLoader =  mock[ActorLoader]
  val actor = system.actorOf(Props(classOf[KernelInfoRequestHandler], actorLoader, LanguageInfo("test", "1.0.0", Some(".test"))))

  val relayProbe : TestProbe = TestProbe()
  val relaySelection : ActorSelection =
    system.actorSelection(relayProbe.ref.path)
  when(actorLoader.load(SystemActorType.KernelMessageRelay))
    .thenReturn(relaySelection)
  when(actorLoader.load(mockNot(mockEq(SystemActorType.KernelMessageRelay))))
    .thenReturn(system.actorSelection(""))

  val header = Header("","","","","")
  val kernelMessage = new KernelMessage(
    Seq[Array[Byte]](), "test message", header, header, Metadata(), "{}"
  )

  describe("Kernel Info Request Handler") {
    it("should return a KernelMessage containing kernel info response") {
      actor ! kernelMessage
      val reply = relayProbe.receiveOne(MaxAkkaTestTimeout).asInstanceOf[KernelMessage]
      val kernelInfo = Json.parse(reply.contentString).as[KernelInfoReply]
      kernelInfo.implementation should be ("spark")
    }
  }
}
