/*
 * Copyright 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.spark.interpreter.broker

import com.ibm.spark.interpreter.broker.producer.{SQLContextProducerLike, JavaSparkContextProducerLike}
import com.ibm.spark.kernel.api.KernelLike
import org.apache.spark.api.java.JavaSparkContext
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers, OneInstancePerTest}
import org.mockito.Mockito._

class BrokerBridgeSpec extends FunSpec with Matchers with OneInstancePerTest
  with MockitoSugar
{
  private val mockBrokerState = mock[BrokerState]
  private val mockKernel = mock[KernelLike]
  private val mockSparkConf = mock[SparkConf]
  private val mockSparkContext = mock[SparkContext]

  private val mockJavaSparkContext = mock[JavaSparkContext]
  doReturn(mockSparkContext).when(mockJavaSparkContext).sc
  private val mockSqlContext = mock[SQLContext]
  doReturn(mockSparkContext).when(mockSqlContext).sparkContext

  // A new SQLContext is created per request, meaning this needs mocking
  doReturn(mockSparkConf).when(mockSparkContext).getConf
  doReturn(Array[(String, String)]()).when(mockSparkConf).getAll

  private val brokerBridge = new BrokerBridge(
    mockBrokerState,
    mockKernel,
    mockSparkContext
  ) with JavaSparkContextProducerLike with SQLContextProducerLike {
    override def newJavaSparkContext(sparkContext: SparkContext): JavaSparkContext = mockJavaSparkContext
    override def newSQLContext(sparkContext: SparkContext): SQLContext = mockSqlContext
  }

  describe("BrokerBridge") {
    describe("#state") {
      it("should return the broker state from the constructor") {
        brokerBridge.state should be (mockBrokerState)
      }
    }

    describe("#javaSparkContext") {
      it("should return a JavaSparkContext wrapping the SparkContext") {
        brokerBridge.javaSparkContext.sc should be (mockSparkContext)
      }
    }

    describe("#sqlContext") {
      it("should return a SQLContext wrapping the SparkContext") {
        brokerBridge.sqlContext.sparkContext should be (mockSparkContext)
      }
    }

    describe("#kernel") {
      it("should return the kernel from the constructor") {
        brokerBridge.kernel should be (mockKernel)
      }
    }

    describe("#sparkConf") {
      it("should return the configuration from the SparkContext") {
        brokerBridge.sparkConf should be (mockSparkConf)
      }
    }
  }
}
