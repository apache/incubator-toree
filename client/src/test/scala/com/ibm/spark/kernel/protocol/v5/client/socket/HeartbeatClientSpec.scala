/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.spark.kernel.protocol.v5.client.socket

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import com.ibm.spark.kernel.protocol.v5.client.ZMQMessage
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpecLike}
import org.mockito.Matchers._
import org.mockito.Mockito._

class HeartbeatClientSpec extends TestKit(ActorSystem("HeartbeatActorSpec"))
  with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {

  describe("HeartbeatClientActor") {
    val socketFactory = mock[SocketFactory]
    val probe : TestProbe = TestProbe()
    when(socketFactory.HeartbeatClient(any(classOf[ActorSystem]), any(classOf[ActorRef]))).thenReturn(probe.ref)

    val heartbeatClient = system.actorOf(Props(classOf[HeartbeatClient], socketFactory))

    describe("send heartbeat") {
      it("should send ping ZMQMessage") {
        heartbeatClient ! HeartbeatMessage
        probe.expectMsgClass(classOf[ZMQMessage])
      }
    }
  }
}
