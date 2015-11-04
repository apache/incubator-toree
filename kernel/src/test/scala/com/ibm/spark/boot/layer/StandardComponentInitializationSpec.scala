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

package com.ibm.spark.boot.layer

import com.ibm.spark.boot.{CommandLineOptions, KernelBootstrap}
import com.ibm.spark.interpreter.Interpreter
import com.ibm.spark.kernel.api.KernelLike
import com.ibm.spark.kernel.protocol.v5.KMBuilder
import com.ibm.spark.kernel.protocol.v5.kernel.ActorLoader
import com.ibm.spark.utils.LogLike
import com.typesafe.config.Config
import org.apache.spark.{SparkConf, SparkContext}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import scala.collection.mutable
import scala.collection.JavaConverters._
class StandardComponentInitializationSpec extends FunSpec with Matchers
  with MockitoSugar with BeforeAndAfter
{
  private val TestAppName = "test app"

  private var mockConfig: Config = _
  private var mockActorLoader: ActorLoader = _
  private var mockSparkContext: SparkContext = _
  private var mockInterpreter: Interpreter = _
  private var mockKernel: KernelLike = _
  private var spyComponentInitialization: StandardComponentInitialization = _

  private class TestComponentInitialization
    extends StandardComponentInitialization with LogLike

  before {
    mockConfig = mock[Config]
    mockActorLoader = mock[ActorLoader]
    mockSparkContext = mock[SparkContext]
    mockInterpreter = mock[Interpreter]
    mockKernel = mock[KernelLike]

    spyComponentInitialization = spy(new TestComponentInitialization())
  }

  describe("StandardComponentInitialization") {
    describe("#initializeInterpreterPlugins") {
      it("should return a map with the DummyInterpreter") {
        val conf = new CommandLineOptions(List(
          "--interpreter-plugin", "dummy:test.utils.DummyInterpreter",
          "--interpreter-plugin", "dummy2:test.utils.DummyInterpreter"
        )).toConfig

        val m = spyComponentInitialization
          .initializeInterpreterPlugins(mockKernel, conf)

        m.get("dummy") should not be None
        m.get("dummy2") should not be None
      }
      it("should return an empty map") {
        val conf = new CommandLineOptions(List()).toConfig

        val m = spyComponentInitialization
          .initializeInterpreterPlugins(mockKernel, conf)

        m.isEmpty shouldBe true
      }
    }
  }
}
