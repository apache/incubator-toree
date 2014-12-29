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

import com.ibm.spark.boot.KernelBootstrap
import com.ibm.spark.interpreter.Interpreter
import com.ibm.spark.kernel.protocol.v5.{KMBuilder, ActorLoader}
import com.ibm.spark.utils.LogLike
import com.typesafe.config.Config
import org.apache.spark.{SparkConf, SparkContext}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

class StandardComponentInitializationSpec extends FunSpec with Matchers
  with MockitoSugar with BeforeAndAfter
{
  private val TestAppName = "test app"

  private var mockConfig: Config = _
  private var mockActorLoader: ActorLoader = _
  private var mockSparkContext: SparkContext = _
  private var mockInterpreter: Interpreter = _
  private var spyComponentInitialization: StandardComponentInitialization = _

  private class TestComponentInitialization
    extends StandardComponentInitialization with LogLike

  before {
    mockConfig = mock[Config]
    mockActorLoader = mock[ActorLoader]
    mockSparkContext = mock[SparkContext]
    mockInterpreter = mock[Interpreter]

    spyComponentInitialization = spy(new TestComponentInitialization())
  }

  describe("StandardComponentInitialization") {
    describe("when spark.master is set in config") {
      it("should set spark.master in SparkConf") {
        val expected = "some value"
        doReturn(expected).when(mockConfig).getString("spark.master")

        // Stub out other helper methods to avoid long init process and to
        // avoid failure when creating SparkContext
        doReturn(mockSparkContext).when(spyComponentInitialization)
          .reallyInitializeSparkContext(
            any[ActorLoader], any[KMBuilder], any[SparkConf])
        doNothing().when(spyComponentInitialization)
          .updateInterpreterWithSparkContext(
            any[Config], any[SparkContext], any[Interpreter])

        // Provide stub for interpreter classServerURI since also executed
        doReturn("").when(mockInterpreter).classServerURI

        val sparkContext = spyComponentInitialization.initializeSparkContext(
          mockConfig, TestAppName, mockActorLoader, mockInterpreter)

        val sparkConf = {
          val sparkConfCaptor = ArgumentCaptor.forClass(classOf[SparkConf])
          verify(spyComponentInitialization).reallyInitializeSparkContext(
            any[ActorLoader], any[KMBuilder], sparkConfCaptor.capture()
          )
          sparkConfCaptor.getValue
        }

        sparkConf.get("spark.master") should be (expected)
      }

      it("should not add ourselves as a jar if spark.master is not local") {
        doReturn("local[*]").when(mockConfig).getString("spark.master")

        spyComponentInitialization.updateInterpreterWithSparkContext(
          mockConfig, mockSparkContext, mockInterpreter)
        verify(mockSparkContext, never()).addJar(anyString())
      }

      it("should add ourselves as a jar if spark.master is not local") {
        doReturn("notlocal").when(mockConfig).getString("spark.master")

        // TODO: This is going to be outdated when we determine a way to
        //       re-include all jars
        val expected =
          com.ibm.spark.SparkKernel.getClass.getProtectionDomain
            .getCodeSource.getLocation.getPath

        spyComponentInitialization.updateInterpreterWithSparkContext(
          mockConfig, mockSparkContext, mockInterpreter)
        verify(mockSparkContext).addJar(expected)
      }
    }
  }
}
