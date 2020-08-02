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

import org.apache.commons.exec.ExecuteException
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers, OneInstancePerTest}
import org.mockito.Mockito._
import org.mockito.Matchers._

class BrokerProcessHandlerSpec extends FunSpec with Matchers
  with OneInstancePerTest with MockitoSugar
{
  private val mockBrokerBridge = mock[BrokerBridge]
  private val brokerProcessHandler = new BrokerProcessHandler(
    mockBrokerBridge,
    restartOnFailure = true,
    restartOnCompletion = true
  )

  describe("BrokerProcessHandler") {
    describe("#onProcessFailed") {
      it("should invoke the reset method") {
        val mockResetMethod = mock[String => Unit]
        brokerProcessHandler.setResetMethod(mockResetMethod)

        brokerProcessHandler.onProcessFailed(mock[ExecuteException])

        verify(mockResetMethod).apply(anyString())
      }

      it("should invoke the restart method if the proper flag is set to true") {
        val mockRestartMethod = mock[() => Unit]
        brokerProcessHandler.setRestartMethod(mockRestartMethod)

        brokerProcessHandler.onProcessFailed(mock[ExecuteException])

        verify(mockRestartMethod).apply()
      }
    }

    describe("#onProcessComplete") {
      it("should invoke the reset method") {
        val mockResetMethod = mock[String => Unit]
        brokerProcessHandler.setResetMethod(mockResetMethod)

        brokerProcessHandler.onProcessComplete(0)

        verify(mockResetMethod).apply(anyString())
      }

      it("should invoke the restart method if the proper flag is set to true") {
        val mockRestartMethod = mock[() => Unit]
        brokerProcessHandler.setRestartMethod(mockRestartMethod)

        brokerProcessHandler.onProcessComplete(0)

        verify(mockRestartMethod).apply()
      }
    }
  }
}
