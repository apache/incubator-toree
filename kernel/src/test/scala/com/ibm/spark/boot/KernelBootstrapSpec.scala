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

package com.ibm.spark.boot

import com.ibm.spark.interpreter.Interpreter
import com.typesafe.config.Config
import org.apache.spark.{SparkConf, SparkContext}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

class KernelBootstrapSpec extends FunSpec with Matchers with MockitoSugar {
  describe("KernelBootstrap") {
    describe("when spark.master is set in config") {
      it("should set spark.master in SparkConf") {
        val config = mock[Config]
        val expectedVal: String = "expected val"
        val bootstrap = spy(new KernelBootstrap(config))
        val captor = ArgumentCaptor.forClass(classOf[SparkConf])

        //  Mocking
        when(config.getString("spark.master")).thenReturn(expectedVal)
        bootstrap.interpreter = mock[Interpreter]
        bootstrap.sparkContext = mock[SparkContext] // Stub out addJar call

        //  Verification
        bootstrap.initializeSparkContext()
        verify(bootstrap).reallyInitializeSparkContext(captor.capture())
        captor.getValue().get("spark.master") should be(expectedVal)
      }

      it("should not add ourselves as a jar if spark.master is not local") {
        val config = mock[Config]
        val sparkMaster: String = "local[*]"
        val bootstrap = spy(new KernelBootstrap(config))
        val captor = ArgumentCaptor.forClass(classOf[SparkConf])

        //  Mocking
        val mockSparkContext = mock[SparkContext]
        when(config.getString("spark.master")).thenReturn(sparkMaster)
        bootstrap.interpreter = mock[Interpreter]
        bootstrap.sparkContext = mockSparkContext

        //  Verification
        bootstrap.initializeSparkContext()
        bootstrap.reallyInitializeSparkContext(captor.capture())
        verify(mockSparkContext, never()).addJar(anyString())
      }

      it("should add ourselves as a jar if spark.master is not local") {
        val config = mock[Config]
        val sparkMaster: String = "notlocal"
        val bootstrap = spy(new KernelBootstrap(config))
        val captor = ArgumentCaptor.forClass(classOf[SparkConf])

        //  Mocking
        val mockSparkContext = mock[SparkContext]
        when(config.getString("spark.master")).thenReturn(sparkMaster)
        bootstrap.interpreter = mock[Interpreter]
        bootstrap.sparkContext = mockSparkContext

        //  Verification
        val expected =
          com.ibm.spark.SparkKernel.getClass.getProtectionDomain
            .getCodeSource.getLocation.getPath
        bootstrap.initializeSparkContext()
        bootstrap.reallyInitializeSparkContext(captor.capture())
        verify(mockSparkContext, times(2)).addJar(expected)
      }
    }
  }
}
