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

package com.ibm.spark.client

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.client.SparkKernelClient
import com.ibm.spark.kernel.protocol.v5.client.execution.ExecuteRequestTuple
import com.ibm.spark.kernel.protocol.v5.content.ExecuteRequest
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}

class SparkKernelClientSpec extends TestKit(ActorSystem("RelayActorSystem"))
  with Matchers with MockitoSugar with FunSpecLike {

  val actorLoader = mock[ActorLoader]
  val client = new SparkKernelClient(actorLoader, system)
  val probe = TestProbe()
  when(actorLoader.load(MessageType.ExecuteRequest))
    .thenReturn(system.actorSelection(probe.ref.path.toString))

  describe("SparkKernelClient") {
    describe("#execute") {
      it("should send an ExecuteRequest message") {
        val func = (x: Any) => println(x)
        client.execute("val foo = 2")
        probe.expectMsgClass(classOf[ExecuteRequestTuple])
      }
    }
  }
}
