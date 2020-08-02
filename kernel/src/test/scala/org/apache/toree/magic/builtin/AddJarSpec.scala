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

package org.apache.toree.magic.builtin

import java.io.{File, OutputStream}
import java.net.{URI, URL}
import java.nio.file.{FileSystems, Files}

import org.apache.toree.interpreter.Interpreter
import org.apache.toree.magic.dependencies.{IncludeConfig, IncludeInterpreter, IncludeKernel, IncludeOutputStream}
import com.typesafe.config.ConfigFactory
import org.apache.spark.SparkContext
import org.apache.toree.kernel.api.KernelLike
import org.apache.toree.plugins.PluginManager
import org.scalatest.{FunSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._

class AddJarSpec extends FunSpec with Matchers with MockitoSugar {
  describe("AddJar"){
    describe("#execute") {
      it("should call addJar on the provided kernel") {
        val mockKernel = mock[KernelLike]
        val mockOutputStream = mock[OutputStream]
        val mockPluginManager = mock[PluginManager]
        val testConfig = ConfigFactory.load()

        val addJarMagic = new AddJar
          with IncludeOutputStream
          with IncludeConfig
          with IncludeKernel
        {
          override val kernel: KernelLike = mockKernel
          override val outputStream: OutputStream = mockOutputStream
          override lazy val pluginManager: PluginManager = mockPluginManager
          override val config = testConfig
        }

        addJarMagic.execute("""https://repo1.maven.org/maven2/org/scala-rules/rule-engine-core_2.11/0.5.1/rule-engine-core_2.11-0.5.1.jar""")

        verify(mockKernel).addJars(any[URI])
        verify(mockPluginManager, times(0)).loadPlugins(any())
      }

      it("should raise exception if jar file does not end in .jar or .zip") {
        val mockOutputStream = mock[OutputStream]

        val addJarMagic = new AddJar
          with IncludeOutputStream
        {
          override val outputStream: OutputStream = mockOutputStream
        }

        intercept[IllegalArgumentException] {
          addJarMagic.execute("""http://www.example.com/""")
        }
        intercept[IllegalArgumentException] {
          addJarMagic.execute("""http://www.example.com/not_a_jar""")
        }
      }

      it("should raise exception if jar file does not exist") {
        val mockOutputStream = mock[OutputStream]

        val addJarMagic = new AddJar
          with IncludeOutputStream
        {
          override val outputStream: OutputStream = mockOutputStream
        }

        intercept[IllegalArgumentException] {
          addJarMagic.execute("""http://ibm.com/this.jar.does.not.exist.jar""")
        }
      }

      it("should extract jar file name from jar URL") {
        val mockOutputStream = mock[OutputStream]

        val addJarMagic = new AddJar
          with IncludeOutputStream
        {
          override val outputStream: OutputStream = mockOutputStream
        }

        var url = """http://www.example.com/someJar.jar"""
        var jarName = addJarMagic.getFileFromLocation(url)
        assert(jarName == "someJar.jar")

        url = """http://www.example.com/remotecontent?filepath=/path/to/someJar.jar"""
        jarName = addJarMagic.getFileFromLocation(url)
        // File names come from the path, not from the query fragment
        assert(jarName == "remotecontent")

        url = """http://www.example.com/"""
        jarName = addJarMagic.getFileFromLocation(url)
        assert(jarName == "")
      }

      it("should use a cached jar if the force option is not provided") {
        val mockKernel = mock[KernelLike]
        val mockOutputStream = mock[OutputStream]
        var downloadFileCalled = false  // Used to verify that downloadFile
                                        // was or was not called in this test
        val testConfig = ConfigFactory.load()

        val addJarMagic = new AddJar
          with IncludeOutputStream
          with IncludeConfig
          with IncludeKernel
        {
          override val kernel: KernelLike = mockKernel
          override val outputStream: OutputStream = mockOutputStream
          override val config = testConfig
          override def downloadFile(fileUrl: URL, destinationUrl: URL): URL = {
            downloadFileCalled = true
            super.downloadFile(fileUrl, destinationUrl)
          }
        }

        addJarMagic.execute("""https://repo1.maven.org/maven2/org/scala-rules/rule-engine-core_2.11/0.5.1/rule-engine-core_2.11-0.5.1.jar""")

        downloadFileCalled should be (false)
        verify(mockKernel).addJars(any[URI])
      }

      it("should not use a cached jar if the force option is provided") {
        val mockKernel = mock[KernelLike]
        val mockOutputStream = mock[OutputStream]
        var downloadFileCalled = false  // Used to verify that downloadFile
                                        // was or was not called in this test
        val testConfig = ConfigFactory.load()

        val addJarMagic = new AddJar
          with IncludeOutputStream
          with IncludeConfig
          with IncludeKernel
        {
          override val kernel: KernelLike = mockKernel
          override val outputStream: OutputStream = mockOutputStream
          override val config = testConfig
          override def downloadFile(fileUrl: URL, destinationUrl: URL): URL = {
            downloadFileCalled = true
            super.downloadFile(fileUrl, destinationUrl)
          }
        }

        addJarMagic.execute("""-f https://repo1.maven.org/maven2/org/scala-rules/rule-engine-core_2.11/0.5.1/rule-engine-core_2.11-0.5.1.jar""")

        downloadFileCalled should be (true)
        verify(mockKernel).addJars(any[URI])
      }

      it("should add magic jar to magicloader and not to interpreter and spark context") {
        val mockSparkContext = mock[SparkContext]
        val mockInterpreter = mock[Interpreter]
        val mockOutputStream = mock[OutputStream]
        val mockPluginManager = mock[PluginManager]
        val testConfig = ConfigFactory.load()

        val addJarMagic = new AddJar
          with IncludeInterpreter
          with IncludeOutputStream
          with IncludeConfig
        {
          override val interpreter: Interpreter = mockInterpreter
          override val outputStream: OutputStream = mockOutputStream
          override lazy val pluginManager: PluginManager = mockPluginManager
          override val config = testConfig
        }

        addJarMagic.execute(
          """--magic https://repo1.maven.org/maven2/org/scala-rules/rule-engine-core_2.11/0.5.1/rule-engine-core_2.11-0.5.1.jar""")

        verify(mockPluginManager).loadPlugins(any())
        verify(mockSparkContext, times(0)).addJar(anyString())
        verify(mockInterpreter, times(0)).addJars(any[URL])
      }
    }
  }
}
