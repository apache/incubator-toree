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

package org.apache.toree.kernel.api

import java.io.{InputStream, PrintStream}
import java.util.concurrent.TimeUnit

import com.typesafe.config.Config
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.toree.boot.layer.InterpreterManager
import org.apache.toree.comm.CommManager
import org.apache.toree.global.ExecuteRequestState
import org.apache.toree.interpreter._
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.plugins.PluginManager
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

class KernelSpec extends FunSpec with Matchers with MockitoSugar
  with BeforeAndAfter
{
  private val BadCode = Some("abc foo bar")
  private val GoodCode = Some("val foo = 1")
  private val ErrorCode = Some("val foo = bar")
  private val ErrorMsg = "Name: error\n" +
    "Message: bad\n" +
    "StackTrace: 1"

  private var mockConfig: Config = _
  private var mockSparkContext: SparkContext = _
  private var mockSparkConf: SparkConf = _
  private var mockActorLoader: ActorLoader = _
  private var mockInterpreter: Interpreter = _
  private var mockInterpreterManager: InterpreterManager = _
  private var mockCommManager: CommManager = _
  private var mockPluginManager: PluginManager = _
  private var kernel: Kernel = _
  private var spyKernel: Kernel = _

  before {
    mockConfig = mock[Config]
    mockInterpreter = mock[Interpreter]
    mockInterpreterManager = mock[InterpreterManager]
    mockSparkContext = mock[SparkContext]
    mockSparkConf = mock[SparkConf]
    mockPluginManager = mock[PluginManager]
    when(mockInterpreterManager.defaultInterpreter)
      .thenReturn(Some(mockInterpreter))
    when(mockInterpreterManager.interpreters)
      .thenReturn(Map[String, org.apache.toree.interpreter.Interpreter]())
    when(mockInterpreter.interpret(BadCode.get))
      .thenReturn((Results.Incomplete, null))
    when(mockInterpreter.interpret(GoodCode.get))
      .thenReturn((Results.Success, Left(Map("text/plain" -> "ok"))))
    when(mockInterpreter.interpret(ErrorCode.get))
      .thenReturn((Results.Error, Right(ExecuteError("error","bad", List("1")))))


    mockCommManager = mock[CommManager]
    mockActorLoader = mock[ActorLoader]

    kernel = new Kernel(
      mockConfig, mockActorLoader, mockInterpreterManager, mockCommManager,
      mockPluginManager
    )

    spyKernel = spy(kernel)

  }

  after {
    ExecuteRequestState.reset()
  }

  describe("Kernel") {
    describe("#eval") {
      it("should return syntax error") {
        kernel eval BadCode should be((false, Map("text/plain" -> "Syntax Error!")))
      }

      it("should return ok") {
        kernel eval GoodCode should be((true, Map("text/plain" -> "ok")))
      }

      it("should return error") {
        kernel eval ErrorCode should be((false, Map("text/plain" -> ErrorMsg)))
      }

      it("should return error on None") {
        kernel eval None should be((false, Map("text/plain" -> "Error!")))
      }
    }

    describe("#out") {
      it("should throw an exception if the ExecuteRequestState has not been set") {
        intercept[IllegalArgumentException] {
          kernel.out
        }
      }

      it("should create a new PrintStream instance if the ExecuteRequestState has been set") {
        ExecuteRequestState.processIncomingKernelMessage(
          new KernelMessage(Nil, "", mock[Header], mock[ParentHeader],
            mock[Metadata], "")
        )
        kernel.out shouldBe a[PrintStream]
      }
    }

    describe("#err") {
      it("should throw an exception if the ExecuteRequestState has not been set") {
        intercept[IllegalArgumentException] {
          kernel.err
        }
      }

      it("should create a new PrintStream instance if the ExecuteRequestState has been set") {
        ExecuteRequestState.processIncomingKernelMessage(
          new KernelMessage(Nil, "", mock[Header], mock[ParentHeader],
            mock[Metadata], "")
        )

        // TODO: Access the underlying streamType field to assert stderr?
        kernel.err shouldBe a[PrintStream]
      }
    }

    describe("#in") {
      it("should throw an exception if the ExecuteRequestState has not been set") {
        intercept[IllegalArgumentException] {
          kernel.in
        }
      }

      it("should create a new InputStream instance if the ExecuteRequestState has been set") {
        ExecuteRequestState.processIncomingKernelMessage(
          new KernelMessage(Nil, "", mock[Header], mock[ParentHeader],
            mock[Metadata], "")
        )

        kernel.in shouldBe a[InputStream]
      }
    }

    describe("#stream") {
      it("should throw an exception if the ExecuteRequestState has not been set") {
        intercept[IllegalArgumentException] {
          kernel.stream
        }
      }

      it("should create a StreamMethods instance if the ExecuteRequestState has been set") {
        ExecuteRequestState.processIncomingKernelMessage(
          new KernelMessage(Nil, "", mock[Header], mock[ParentHeader],
            mock[Metadata], "")
        )

        kernel.stream shouldBe a[StreamMethods]
      }
    }

    describe("#display") {
      it("should throw an exception if the ExecuteRequestState has not been set") {
        intercept[IllegalArgumentException] {
          kernel.display
        }
      }

      it("should create a DisplayMethods instance if the ExecuteRequestState has been set") {
        ExecuteRequestState.processIncomingKernelMessage(
          new KernelMessage(Nil, "", mock[Header], mock[ParentHeader],
            mock[Metadata], "")
        )

        kernel.display shouldBe a[DisplayMethods]
      }
    }

    describe("when spark.master is set in config") {

      it("should create SparkConf") {
        val expected = "some value"
        doReturn(expected).when(mockConfig).getString("spark.master")

        val sparkConf = kernel.createSparkConf(new SparkConf().setMaster(expected))

        sparkConf.get("spark.master") should be(expected)
      }
    }

    describe("when spark-context-initialization-timeout is a valid value") {

      it("should use the specified timeout to initialize spark context") {
        val expectedTimeout: Long = 30000
        doReturn(expectedTimeout).when(mockConfig).getDuration("spark_context_initialization_timeout", TimeUnit.MILLISECONDS)

        kernel.getSparkContextInitializationTimeout should be(expectedTimeout)
      }

      it("should throw an exception when negative value is specified as timeout") {
        intercept[RuntimeException] {
          val timeout: Long = -30000
          doReturn(timeout).when(mockConfig).getDuration("spark_context_initialization_timeout", TimeUnit.MILLISECONDS)

          kernel.getSparkContextInitializationTimeout
        }
      }

      it("should throw an exception when zero is specified as timeout") {
        intercept[RuntimeException] {
          val timeout: Long = 0
          doReturn(timeout).when(mockConfig).getDuration("spark_context_initialization_timeout", TimeUnit.MILLISECONDS)

          kernel.getSparkContextInitializationTimeout
        }
      }
    }
  }
}
