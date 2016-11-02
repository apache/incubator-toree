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

import org.scalatest.{FunSpec, Matchers, OneInstancePerTest}

import scala.util.Success

class BrokerStateSpec extends FunSpec with Matchers with OneInstancePerTest {

  private val TestMaxQueuedCode = 5
  private val brokerState = new BrokerState(TestMaxQueuedCode)

  describe("BrokerState") {
    describe("#pushCode") {
      it("should throw an exception if the queue is maxed out") {
        val code = "some code"

        // Fill up to the max of our queue
        for (i <- 1 to TestMaxQueuedCode)
          brokerState.pushCode(code)

        // Adding an additional code should throw an exception
        intercept[IllegalStateException] {
          brokerState.pushCode(code)
        }
      }

      it("should queue up the code to eventually be executed") {
        val code = "some code"

        brokerState.totalQueuedCode() should be (0)
        brokerState.pushCode(code)
        brokerState.totalQueuedCode() should be (1)
      }
    }

    describe("#totalQueuedCode") {
      it("should return the total queued code elements") {
        val code = "some code"

        // Queue up to the maximum test elements, verifying that the total
        // queued element count increases per push
        for (i <- 1 to TestMaxQueuedCode) {
          brokerState.pushCode(code)
          brokerState.totalQueuedCode() should be (i)
        }
      }
    }

    describe("#nextCode") {
      it("should return the next code element if available") {
        val code = "some code"

        brokerState.pushCode(code)

        brokerState.nextCode().code should be (code)
      }

      it("should return null if no code element is available") {
        brokerState.nextCode() should be (null)
      }
    }

    describe("#isReady") {
      it("should return true if the broker state is marked as ready") {
        brokerState.markReady()
        brokerState.isReady should be (true)
      }

      it("should return false if the broker state is not marked as ready") {
        brokerState.isReady should be (false)
      }
    }

    describe("#markReady") {
      it("should mark the state of the broker as ready") {
        // Mark once to make sure that the state gets set
        brokerState.markReady()
        brokerState.isReady should be (true)

        // Mark a second time to ensure that the state does not change
        brokerState.markReady()
        brokerState.isReady should be (true)
      }
    }

    describe("#markSuccess") {
      it("should mark the future for the code as successful") {
        val future = brokerState.pushCode("some code")
        val BrokerCode(codeId, _) = brokerState.nextCode()

        brokerState.markSuccess(codeId)
        future.value.get.isSuccess should be (true)
      }

      it("should use the provided message as the contents of the future") {
        val future = brokerState.pushCode("some code")
        val BrokerCode(codeId, _) = brokerState.nextCode()

        val message = "some message"
        brokerState.markSuccess(codeId, message)
        future.value.get should be (Success(message))
      }

      it("should do nothing if the code id is invalid") {
        val future = brokerState.pushCode("some code")
        val BrokerCode(codeId, _) = brokerState.nextCode()

        brokerState.markSuccess(codeId + "1")
        future.isCompleted should be (false)
      }
    }

    describe("#markFailure") {
      it("should mark the future for the code as failure") {
        val future = brokerState.pushCode("some code")
        val BrokerCode(codeId, _) = brokerState.nextCode()

        brokerState.markFailure(codeId)
        future.value.get.isSuccess should be (false)
      }

      it("should use the provided message as the contents of the exception") {
        val future = brokerState.pushCode("some code")
        val BrokerCode(codeId, _) = brokerState.nextCode()

        val message = "some message"
        brokerState.markFailure(codeId, message)

        val failure = future.value.get.failed.get
        failure.getLocalizedMessage should be (message)
      }

      it("should do nothing if the code id is invalid") {
        val future = brokerState.pushCode("some code")
        val BrokerCode(codeId, _) = brokerState.nextCode()

        brokerState.markFailure(codeId + "1")
        future.isCompleted should be (false)
      }
    }

    describe("#reset") {
      it("should clear any code still in the queue") {
        brokerState.pushCode("some code")

        brokerState.reset("")

        brokerState.totalQueuedCode() should be (0)
      }

      it("should mark any evaluating code as a failure if marked true") {
        val future = brokerState.pushCode("some code")

        brokerState.reset("")

        future.value.get.isFailure should be (true)
      }

      it("should use the message as the contents of the failed code futures") {
        val future = brokerState.pushCode("some code")

        val message = "some message"
        brokerState.reset(message)

        val failure = future.value.get.failed.get
        failure.getLocalizedMessage should be (message)
      }

      it("should mark any evaluating code as a success if marked false") {
        val future = brokerState.pushCode("some code")

        brokerState.reset("", markAllAsFailure = false)

        future.value.get.isSuccess should be (true)
      }

      it("should use the message as the contents of the successful code futures") {
        val future = brokerState.pushCode("some code")

        val message = "some message"
        brokerState.reset(message, markAllAsFailure = false)

        future.value.get should be (Success(message))
      }
    }
  }
}
