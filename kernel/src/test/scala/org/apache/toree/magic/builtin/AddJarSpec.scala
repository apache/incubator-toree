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

package org.apache.toree.magic.builtin

import java.io.OutputStream
import java.net.URL
import java.nio.file.{FileSystems, Files}

import org.apache.toree.interpreter.Interpreter
import org.apache.toree.magic.dependencies.{IncludeConfig, IncludeOutputStream, IncludeInterpreter, IncludeSparkContext}
import com.typesafe.config.ConfigFactory
import org.apache.spark.SparkContext
import org.scalatest.{Matchers, FunSpec}
import org.scalatest.mock.MockitoSugar

import org.mockito.Mockito._
import org.mockito.Matchers._
import org.apache.toree.magic.MagicLoader

class AddJarSpec extends FunSpec with Matchers with MockitoSugar {
  describe("AddJar"){
    describe("#execute") {
      it("should call addJar on the provided SparkContext and addJars on the " +
         "provided interpreter") {
        val mockSparkContext = mock[SparkContext]
        val mockInterpreter = mock[Interpreter]
        val mockOutputStream = mock[OutputStream]
        val mockMagicLoader = mock[MagicLoader]
        val testConfig = ConfigFactory.load()

        val addJarMagic = new AddJar
          with IncludeSparkContext
          with IncludeInterpreter
          with IncludeOutputStream
          with IncludeConfig
        {
          override val sparkContext: SparkContext = mockSparkContext
          override val interpreter: Interpreter = mockInterpreter
          override val outputStream: OutputStream = mockOutputStream
          override lazy val magicLoader: MagicLoader = mockMagicLoader
          override val config = testConfig
          override def downloadFile(fileUrl: URL, destinationUrl: URL): URL =
            new URL("file://someFile") // Cannot mock URL
        }

        addJarMagic.execute("""http://www.example.com/someJar.jar""")

        verify(mockSparkContext).addJar(anyString())
        verify(mockInterpreter).addJars(any[URL])
        verify(mockMagicLoader, times(0)).addJar(any())
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
        assert(jarName == "someJar.jar")

        url = """http://www.example.com/"""
        jarName = addJarMagic.getFileFromLocation(url)
        assert(jarName == "")
      }

      it("should use a cached jar if the force option is not provided") {
        val mockSparkContext = mock[SparkContext]
        val mockInterpreter = mock[Interpreter]
        val mockOutputStream = mock[OutputStream]
        var downloadFileCalled = false  // Used to verify that downloadFile
                                        // was or was not called in this test
        val testConfig = ConfigFactory.load()

        val addJarMagic = new AddJar
          with IncludeSparkContext
          with IncludeInterpreter
          with IncludeOutputStream
          with IncludeConfig
        {
          override val sparkContext: SparkContext = mockSparkContext
          override val interpreter: Interpreter = mockInterpreter
          override val outputStream: OutputStream = mockOutputStream
          override val config = testConfig
          override def downloadFile(fileUrl: URL, destinationUrl: URL): URL = {
            downloadFileCalled = true
            new URL("file://someFile") // Cannot mock URL
          }
        }

        // Create a temporary file representing our jar to fake the cache
        val tmpFilePath = Files.createTempFile(
          FileSystems.getDefault.getPath(AddJar.getJarDir(testConfig)),
          "someJar",
          ".jar"
        )

        addJarMagic.execute(
          """http://www.example.com/""" + tmpFilePath.getFileName)

        tmpFilePath.toFile.delete()

        downloadFileCalled should be (false)
        verify(mockSparkContext).addJar(anyString())
        verify(mockInterpreter).addJars(any[URL])
      }

      it("should not use a cached jar if the force option is provided") {
        val mockSparkContext = mock[SparkContext]
        val mockInterpreter = mock[Interpreter]
        val mockOutputStream = mock[OutputStream]
        var downloadFileCalled = false  // Used to verify that downloadFile
                                        // was or was not called in this test
        val testConfig = ConfigFactory.load()

        val addJarMagic = new AddJar
          with IncludeSparkContext
          with IncludeInterpreter
          with IncludeOutputStream
          with IncludeConfig
        {
          override val sparkContext: SparkContext = mockSparkContext
          override val interpreter: Interpreter = mockInterpreter
          override val outputStream: OutputStream = mockOutputStream
          override val config = testConfig
          override def downloadFile(fileUrl: URL, destinationUrl: URL): URL = {
            downloadFileCalled = true
            new URL("file://someFile") // Cannot mock URL
          }
        }

        // Create a temporary file representing our jar to fake the cache
        val tmpFilePath = Files.createTempFile(
          FileSystems.getDefault.getPath(AddJar.getJarDir(testConfig)),
          "someJar",
          ".jar"
        )

        addJarMagic.execute(
          """-f http://www.example.com/""" + tmpFilePath.getFileName)

        tmpFilePath.toFile.delete()

        downloadFileCalled should be (true)
        verify(mockSparkContext).addJar(anyString())
        verify(mockInterpreter).addJars(any[URL])
      }

      it("should add magic jar to magicloader and not to interpreter and spark"+
         "context") {
        val mockSparkContext = mock[SparkContext]
        val mockInterpreter = mock[Interpreter]
        val mockOutputStream = mock[OutputStream]
        val mockMagicLoader = mock[MagicLoader]
        val testConfig = ConfigFactory.load()

        val addJarMagic = new AddJar
          with IncludeSparkContext
          with IncludeInterpreter
          with IncludeOutputStream
          with IncludeConfig
        {
          override val sparkContext: SparkContext = mockSparkContext
          override val interpreter: Interpreter = mockInterpreter
          override val outputStream: OutputStream = mockOutputStream
          override lazy val magicLoader: MagicLoader = mockMagicLoader
          override val config = testConfig
          override def downloadFile(fileUrl: URL, destinationUrl: URL): URL =
            new URL("file://someFile") // Cannot mock URL
        }

        addJarMagic.execute(
          """--magic http://www.example.com/someJar.jar""")

        verify(mockMagicLoader).addJar(any())
        verify(mockSparkContext, times(0)).addJar(anyString())
        verify(mockInterpreter, times(0)).addJars(any[URL])
      }
    }
  }
}
