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

package org.apache.toree.comm

import org.apache.toree.kernel.protocol.v5
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.client.ActorLoader
import org.apache.toree.kernel.protocol.v5.content.CommContent
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

class ClientCommManagerSpec extends FunSpec with Matchers with BeforeAndAfter
  with MockitoSugar
{
  private val TestTargetName = "some target"

  private var mockActorLoader: ActorLoader = _
  private var mockKMBuilder: KMBuilder = _
  private var mockCommRegistrar: CommRegistrar = _
  private var clientCommManager: ClientCommManager = _

  private var generatedCommWriter: CommWriter = _

  before {
    mockActorLoader = mock[ActorLoader]
    mockKMBuilder = mock[KMBuilder]
    mockCommRegistrar = mock[CommRegistrar]

    clientCommManager = new ClientCommManager(
      mockActorLoader,
      mockKMBuilder,
      mockCommRegistrar
    ) {
      override protected def newCommWriter(commId: UUID): CommWriter = {
        val commWriter = super.newCommWriter(commId)

        generatedCommWriter = commWriter

        val spyCommWriter = spy(commWriter)
        doNothing().when(spyCommWriter)
          .sendCommKernelMessage(any[KernelMessageContent with CommContent])

        spyCommWriter
      }
    }
  }

  describe("ClientCommManager") {
    describe("#open") {
      it("should return a wrapped instance of ClientCommWriter") {
        clientCommManager.open(TestTargetName, v5.MsgData.Empty)

        // Exposed hackishly for testing
        generatedCommWriter shouldBe a [ClientCommWriter]
      }
    }
  }
}
