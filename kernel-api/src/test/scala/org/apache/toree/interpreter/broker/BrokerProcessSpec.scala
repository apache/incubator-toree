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

import java.io.{OutputStream, InputStream, File}

import org.apache.commons.exec._
import org.apache.commons.io.FilenameUtils
import org.mockito.ArgumentCaptor
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers, OneInstancePerTest}

import org.mockito.Mockito._
import org.mockito.Matchers._

class BrokerProcessSpec extends FunSpec with Matchers
  with OneInstancePerTest with MockitoSugar
{
  private val TestProcessName = "test_process"
  private val TestEntryResource = "test/entry/resource"
  private val TestOtherResources = Seq("test/resource/1", "test/resource/2")
  private val TestArguments = Seq("a", "b", "c")
  private val TestEnvironment = Map(
    "e1" -> "1",
    "e2" -> "2"
  )

  private val mockBrokerBridge = mock[BrokerBridge]
  private val mockBrokerProcessHandler = mock[BrokerProcessHandler]

  private val mockExecutor = mock[Executor]

  private val brokerProcess = new BrokerProcess(
    processName = TestProcessName,
    entryResource = TestEntryResource,
    otherResources = TestOtherResources,
    brokerBridge = mockBrokerBridge,
    brokerProcessHandler = mockBrokerProcessHandler,
    arguments = TestArguments
  ) {
    @volatile private var _tmpDir: String =
      System.getProperty("java.io.tmpdir")

    def setTmpDirectory(newDir: String) = _tmpDir = newDir
    override protected def getTmpDirectory: String = _tmpDir
    override protected def newExecutor(): Executor = mockExecutor
    override protected def copy(
      inputStream: InputStream,
      outputStream: OutputStream
    ): Int = 0

    override protected def newProcessEnvironment(): Map[String, String] =
      TestEnvironment
    override protected lazy val getSubDirectory: String = ""
    def doCopyResourceToTmp(resource: String) = copyResourceToTmp(resource)
  }

  describe("BrokerProcess") {
    describe("constructor") {
      it("should fail if the process name is null") {
        intercept[IllegalArgumentException] {
          new BrokerProcess(
            processName = null,
            entryResource = TestEntryResource,
            otherResources = TestOtherResources,
            brokerBridge = mockBrokerBridge,
            brokerProcessHandler = mockBrokerProcessHandler,
            arguments = TestArguments
          )
        }
      }

      it("should fail if the process name is empty") {
        intercept[IllegalArgumentException] {
          new BrokerProcess(
            processName = " \t\n\r",
            entryResource = TestEntryResource,
            otherResources = TestOtherResources,
            brokerBridge = mockBrokerBridge,
            brokerProcessHandler = mockBrokerProcessHandler,
            arguments = TestArguments
          )
        }
      }

      it("should fail if the entry resource is null") {
        intercept[IllegalArgumentException] {
          new BrokerProcess(
            processName = TestProcessName,
            entryResource = null,
            otherResources = TestOtherResources,
            brokerBridge = mockBrokerBridge,
            brokerProcessHandler = mockBrokerProcessHandler,
            arguments = TestArguments
          )
        }
      }

      it("should fail if the entry resource is empty") {
        intercept[IllegalArgumentException] {
          new BrokerProcess(
            processName = TestProcessName,
            entryResource = " \t\n\r",
            otherResources = TestOtherResources,
            brokerBridge = mockBrokerBridge,
            brokerProcessHandler = mockBrokerProcessHandler,
            arguments = TestArguments
          )
        }
      }
    }

    describe("#copyResourceToTmp") {
      it("should fail if a directory with the resource name already exists") {
        val baseDir = System.getProperty("java.io.tmpdir")
        val newResourceName = "some_resource/"

        val resourceFile = new File(baseDir + s"/$newResourceName")
        resourceFile.delete() // Ensure that there is not a file or something
        resourceFile.mkdir()

        intercept[BrokerException] {
          brokerProcess.doCopyResourceToTmp(resourceFile.getPath)
        }

        resourceFile.delete()
      }

      it("should throw an exception if the tmp directory is not set") {
        brokerProcess.setTmpDirectory(null)

        intercept[BrokerException] {
          brokerProcess.doCopyResourceToTmp("some file")
        }
      }

      it("should return the resulting destination of the resource") {
        val rootDir = System.getProperty("java.io.tmpdir")
        val fileName = FilenameUtils.getBaseName(TestEntryResource)
        val fullPath = Seq(rootDir, fileName).mkString("/")
        val expected = new File(fullPath)

        expected.delete()

        brokerProcess.setTmpDirectory(rootDir)
        val destination = brokerProcess.doCopyResourceToTmp(TestEntryResource)

        val actual = new File(destination)
        actual should be (expected)
      }
    }

    describe("#start") {
      it("should throw an exception if the process is already started") {
        brokerProcess.start()

        intercept[AssertionError] {
          brokerProcess.start()
        }
      }

      it("should execute the process using the entry and provided arguments") {
        val finalResourceDestination = FilenameUtils.concat(
          System.getProperty("java.io.tmpdir"),
          FilenameUtils.getBaseName(TestEntryResource)
        )
        val expected = finalResourceDestination +: TestArguments

        val commandLineCaptor = ArgumentCaptor.forClass(classOf[CommandLine])

        brokerProcess.start()
        verify(mockExecutor).execute(commandLineCaptor.capture(), any(), any())

        val commandLine = commandLineCaptor.getValue
        val actual = commandLine.getArguments

        actual should contain theSameElementsAs expected
      }

      it("should execute using the environment provided") {
        val finalResourceDestination = FilenameUtils.concat(
          System.getProperty("java.io.tmpdir"),
          FilenameUtils.getBaseName(TestEntryResource)
        )

        val environmentCaptor =
          ArgumentCaptor.forClass(classOf[java.util.Map[String, String]])

        brokerProcess.start()
        verify(mockExecutor).execute(any(),environmentCaptor.capture() , any())

        import scala.collection.JavaConverters._
        val environment = environmentCaptor.getValue.asScala

        environment should contain theSameElementsAs TestEnvironment
      }

      it("should use the process handler provided to listen for events") {
        val expected = mockBrokerProcessHandler
        val finalResourceDestination = FilenameUtils.concat(
          System.getProperty("java.io.tmpdir"),
          FilenameUtils.getBaseName(TestEntryResource)
        )

        val executeRequestHandlerCaptor =
          ArgumentCaptor.forClass(classOf[ExecuteResultHandler])

        brokerProcess.start()
        verify(mockExecutor).execute(
          any(), any(), executeRequestHandlerCaptor.capture())

        val actual = executeRequestHandlerCaptor.getValue
        actual should be (expected)
      }
    }

    describe("#stop") {
      it("should destroy the process if it is running") {
        brokerProcess.start()

        val mockExecuteWatchdog = mock[ExecuteWatchdog]
        doReturn(mockExecuteWatchdog).when(mockExecutor).getWatchdog

        brokerProcess.stop()

        verify(mockExecuteWatchdog).destroyProcess()
      }

      it("should not try to destroy the process if it is not running") {
        val mockExecuteWatchdog = mock[ExecuteWatchdog]
        doReturn(mockExecuteWatchdog).when(mockExecutor).getWatchdog

        brokerProcess.stop()

        verify(mockExecuteWatchdog, never()).destroyProcess()
      }
    }
  }
}
