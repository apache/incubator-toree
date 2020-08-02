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
package org.apache.toree.plugins

import java.lang.reflect.Method

import org.apache.toree.plugins.annotations.{Priority, Event}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{OneInstancePerTest, Matchers, FunSpec}
import org.mockito.Mockito._

import scala.util.{Failure, Success, Try}

class PluginMethodResultSpec extends FunSpec with Matchers
  with OneInstancePerTest with MockitoSugar
{
  private val testResult = new AnyRef
  private val testThrowable = new Throwable

  @Priority(level = 998)
  private class TestPlugin extends Plugin {
    @Priority(level = 999)
    @Event(name = "success")
    def success() = testResult

    @Event(name = "failure")
    def failure() = throw testThrowable
  }

  private val testPlugin = new TestPlugin

  private val successResult: PluginMethodResult = SuccessPluginMethodResult(
    testPlugin.eventMethodMap("success").head,
    testResult
  )

  private val failureResult: PluginMethodResult = FailurePluginMethodResult(
    testPlugin.eventMethodMap("failure").head,
    testThrowable
  )

  describe("PluginMethodResult") {
    describe("#pluginName") {
      it("should return the name of the plugin from the invoked plugin method") {
        val expected = testPlugin.name

        val actual = successResult.pluginName

        actual should be (expected)
      }
    }

    describe("#methodName") {
      it("should return the name of the method from the invoked plugin method") {
        val expected = "success"

        val actual = successResult.methodName

        actual should be (expected)
      }
    }

    describe("#pluginPriority") {
      it("should return the priority of the plugin from the invoked plugin method") {
        val expected = 998

        val actual = successResult.pluginPriority

        actual should be (expected)
      }
    }

    describe("#methodPriority") {
      it("should return the priority of the method from the invoked plugin method") {
        val expected = 999

        val actual = successResult.methodPriority

        actual should be (expected)
      }
    }

    describe("#isSuccess") {
      it("should return true if representing a success result") {
        val expected = true

        val actual = successResult.isSuccess

        actual should be (expected)
      }

      it("should return false if representing a failure result") {
        val expected = false

        val actual = failureResult.isSuccess

        actual should be (expected)
      }
    }

    describe("#isFailure") {
      it("should return false if representing a success result") {
        val expected = false

        val actual = successResult.isFailure

        actual should be (expected)
      }

      it("should return true if representing a failure result") {
        val expected = true

        val actual = failureResult.isFailure

        actual should be (expected)
      }
    }

    describe("#toTry") {
      it("should return Success(result) if representing a success result") {
        val expected = Success(testResult)

        val actual = successResult.toTry

        actual should be (expected)
      }

      it("should return Failure(throwable) if representing a failure result") {
        val expected = Failure(testThrowable)

        val actual = failureResult.toTry

        actual should be (expected)
      }
    }
  }
}
