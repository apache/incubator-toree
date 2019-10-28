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

package org.apache.toree.kernel.protocol.v5.client

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.apache.toree.comm.{CommCallbacks, CommStorage, CommRegistrar}
import org.apache.toree.kernel.protocol.v5
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.client.execution.ExecuteRequestTuple
import scala.concurrent.duration._
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => mockEq, _}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}

class SparkKernelClientSpec
  extends TestKit(ActorSystem("SparkKernelClientActorSystem"))
  with Matchers with MockitoSugar with FunSpecLike with BeforeAndAfter
{
  private val TestTargetName = "some target"

  private var mockActorLoader: ActorLoader = _
  private var mockCommRegistrar: CommRegistrar = _
  private var sparkKernelClient: SparkKernelClient = _
  private var executeRequestProbe: TestProbe = _
  private var shellClientProbe: TestProbe = _

  before {
    mockActorLoader = mock[ActorLoader]
    mockCommRegistrar = mock[CommRegistrar]

    executeRequestProbe = TestProbe()
    when(mockActorLoader.load(MessageType.Incoming.ExecuteRequest))
      .thenReturn(system.actorSelection(executeRequestProbe.ref.path.toString))

    shellClientProbe = TestProbe()
    when(mockActorLoader.load(SocketType.ShellClient))
      .thenReturn(system.actorSelection(shellClientProbe.ref.path.toString))

    sparkKernelClient = new SparkKernelClient(
      mockActorLoader, system, mockCommRegistrar)
  }

  describe("SparkKernelClient") {
    describe("#execute") {
      it("should send an ExecuteRequest message") {
        val func = (x: Any) => println(x)
        sparkKernelClient.execute("val foo = 2")
        executeRequestProbe.expectMsgClass(classOf[ExecuteRequestTuple])
      }
    }
  }
}
