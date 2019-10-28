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
import com.typesafe.config.ConfigFactory
import org.apache.toree.comm.CommStorage
import org.apache.toree.kernel.protocol.v5.content.CommInfoReply
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.kernel.protocol.v5.{Header, KernelMessage, Metadata, SystemActorType}
import org.mockito.AdditionalMatchers.{not => mockNot}
import org.mockito.Matchers.{eq => mockEq}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}
import play.api.libs.json.Json

import scala.concurrent.duration._

object CommInfoRequestHandlerSpec {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class CommInfoRequestHandlerSpec extends TestKit(
  ActorSystem("CommInfoRequestHandlerSpec",
    ConfigFactory.parseString(CommInfoRequestHandlerSpec.config),
    org.apache.toree.Main.getClass.getClassLoader
  )
) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {

  var WAIT_TIME = 2.seconds

  var mockCommStorage: CommStorage = mock[CommStorage]

  val actorLoader: ActorLoader =  mock[ActorLoader]
  val actor = system.actorOf(Props(classOf[CommInfoRequestHandler], actorLoader, mockCommStorage))

  val relayProbe : TestProbe = TestProbe()
  val relaySelection : ActorSelection =
    system.actorSelection(relayProbe.ref.path)
  when(actorLoader.load(SystemActorType.KernelMessageRelay))
    .thenReturn(relaySelection)
  when(actorLoader.load(mockNot(mockEq(SystemActorType.KernelMessageRelay))))
    .thenReturn(system.actorSelection(""))

  val header = Header("","","","","")

  describe("Comm Info Request Handler") {
    it("should return a KernelMessage containing a comm info response for a specific target name") {
      val kernelMessage = new KernelMessage(
        Seq[Array[Byte]](), "test message", header, header, Metadata(), "{\"target_name\":\"test.name\"}"
      )

      when(mockCommStorage.getTargets()).thenReturn(Set("test.name"))
      when(mockCommStorage.getCommIdsFromTarget("test.name")).thenReturn(Option(scala.collection.immutable.IndexedSeq("1", "2")))

      actor ! kernelMessage
      val reply = relayProbe.receiveOne(WAIT_TIME).asInstanceOf[KernelMessage]
      val commInfo = Json.parse(reply.contentString).as[CommInfoReply]

      commInfo.comms.size should equal (2)
      commInfo.comms.get("1").get.get("target_name").get should be ("test.name")
      commInfo.comms.get("2").get.get("target_name").get should be ("test.name")
    }
  }

  it("should return a KernelMessage containing a comm info response for all comms when target_name is missing from the message") {
    val kernelMessage = new KernelMessage(
      Seq[Array[Byte]](), "test message", header, header, Metadata(), "{}"
    )

    when(mockCommStorage.getTargets()).thenReturn(Set("test.name1", "test.name2"))
    when(mockCommStorage.getCommIdsFromTarget("test.name1")).thenReturn(Option(scala.collection.immutable.IndexedSeq("1", "2")))
    when(mockCommStorage.getCommIdsFromTarget("test.name2")).thenReturn(Option(scala.collection.immutable.IndexedSeq("3", "4")))

    actor ! kernelMessage
    val reply = relayProbe.receiveOne(WAIT_TIME).asInstanceOf[KernelMessage]
    val commInfo = Json.parse(reply.contentString).as[CommInfoReply]

    commInfo.comms.size should equal (4)
    commInfo.comms.get("1").get.get("target_name").get should be ("test.name1")
    commInfo.comms.get("2").get.get("target_name").get should be ("test.name1")
    commInfo.comms.get("3").get.get("target_name").get should be ("test.name2")
    commInfo.comms.get("4").get.get("target_name").get should be ("test.name2")
  }

  it("should return a KernelMessage containing an empty comm info response when the target name value is not found") {
    val kernelMessage = new KernelMessage(
      Seq[Array[Byte]](), "test message", header, header, Metadata(), "{\"target_name\":\"can't_find_me\"}"
    )

    when(mockCommStorage.getTargets()).thenReturn(Set("test.name"))
    when(mockCommStorage.getCommIdsFromTarget("test.name")).thenReturn(Option(scala.collection.immutable.IndexedSeq("1", "2")))

    actor ! kernelMessage
    val reply = relayProbe.receiveOne(WAIT_TIME).asInstanceOf[KernelMessage]
    val commInfo = Json.parse(reply.contentString).as[CommInfoReply]

    commInfo.comms.size should equal (0)
  }
}
