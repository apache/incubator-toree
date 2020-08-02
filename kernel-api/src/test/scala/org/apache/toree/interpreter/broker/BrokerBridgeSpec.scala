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
package org.apache.toree.interpreter.broker

import org.apache.toree.kernel.api.KernelLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers, OneInstancePerTest}

class BrokerBridgeSpec extends FunSpec with Matchers with OneInstancePerTest
  with MockitoSugar
{
  private val mockBrokerState = mock[BrokerState]
  private val mockKernel = mock[KernelLike]

  private val brokerBridge = new BrokerBridge(
    mockBrokerState,
    mockKernel
  )

  describe("BrokerBridge") {
    describe("#state") {
      it("should return the broker state from the constructor") {
        brokerBridge.state should be (mockBrokerState)
      }
    }

    describe("#kernel") {
      it("should return the kernel from the constructor") {
        brokerBridge.kernel should be (mockKernel)
      }
    }
  }
}
