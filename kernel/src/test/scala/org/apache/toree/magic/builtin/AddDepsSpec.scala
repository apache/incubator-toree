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

import java.io.{ByteArrayOutputStream, OutputStream}
import java.net.URL

import org.apache.toree.dependencies.DependencyDownloader
import org.apache.toree.interpreter.Interpreter
import org.apache.toree.utils.ArgumentParsingSupport
import org.apache.spark.SparkContext
import org.scalatest.mock.MockitoSugar
import org.scalatest.{GivenWhenThen, Matchers, FunSpec}
import org.mockito.Mockito._
import org.mockito.Matchers._

import org.apache.toree.magic._
import org.apache.toree.magic.dependencies._

class AddDepsSpec extends FunSpec with Matchers with MockitoSugar
  with GivenWhenThen
{
  describe("AddDeps"){
    describe("#execute") {
      it("should print out the help message if the input is invalid") {
        val byteArrayOutputStream = new ByteArrayOutputStream()
        val mockIntp = mock[Interpreter]
        val mockSC = mock[SparkContext]
        val mockDownloader = mock[DependencyDownloader]
        var printHelpWasRun = false

        val addDepsMagic = new AddDeps
          with IncludeSparkContext
          with IncludeInterpreter
          with IncludeOutputStream
          with IncludeDependencyDownloader
          with ArgumentParsingSupport
        {
          override val sparkContext: SparkContext = mockSC
          override val interpreter: Interpreter = mockIntp
          override val dependencyDownloader: DependencyDownloader =
            mockDownloader
          override val outputStream: OutputStream = byteArrayOutputStream

          override def printHelp(
            outputStream: OutputStream, usage: String
          ): Unit = printHelpWasRun = true
        }

        val expected = LineMagicOutput
        val actual = addDepsMagic.execute("notvalid")

        printHelpWasRun should be (true)
        verify(mockIntp, times(0)).addJars(any())
        verify(mockIntp, times(0)).bind(any(), any(), any(), any())
        verify(mockSC, times(0)).addJar(any())
        verify(mockDownloader, times(0)).retrieve(
          anyString(), anyString(), anyString(), anyBoolean(), anyBoolean())
        actual should be (expected)
      }

      it("should set the retrievals transitive to true if provided") {
        val mockDependencyDownloader = mock[DependencyDownloader]
        doReturn(Nil).when(mockDependencyDownloader).retrieve(
          anyString(), anyString(), anyString(), anyBoolean(), anyBoolean())

        val addDepsMagic = new AddDeps
          with IncludeSparkContext
          with IncludeInterpreter
          with IncludeOutputStream
          with IncludeDependencyDownloader
          with ArgumentParsingSupport
        {
          override val sparkContext: SparkContext = mock[SparkContext]
          override val interpreter: Interpreter = mock[Interpreter]
          override val dependencyDownloader: DependencyDownloader =
            mockDependencyDownloader
          override val outputStream: OutputStream = mock[OutputStream]
        }

        val expected = "org.apache.toree" :: "kernel" :: "1.0" :: "--transitive" :: Nil
        addDepsMagic.execute(expected.mkString(" "))

        verify(mockDependencyDownloader).retrieve(
          expected(0), expected(1), expected(2), true)
      }

      it("should set the retrieval's transitive to false if not provided") {
        val mockDependencyDownloader = mock[DependencyDownloader]
        doReturn(Nil).when(mockDependencyDownloader).retrieve(
          anyString(), anyString(), anyString(), anyBoolean(), anyBoolean())

        val addDepsMagic = new AddDeps
          with IncludeSparkContext
          with IncludeInterpreter
          with IncludeOutputStream
          with IncludeDependencyDownloader
          with ArgumentParsingSupport
        {
          override val sparkContext: SparkContext = mock[SparkContext]
          override val interpreter: Interpreter = mock[Interpreter]
          override val dependencyDownloader: DependencyDownloader =
            mockDependencyDownloader
          override val outputStream: OutputStream = mock[OutputStream]
        }

        val expected = "org.apache.toree" :: "kernel" :: "1.0" :: Nil
        addDepsMagic.execute(expected.mkString(" "))

        verify(mockDependencyDownloader).retrieve(
          expected(0), expected(1), expected(2), false)
      }

      it("should add retrieved artifacts to the interpreter") {
        val mockDependencyDownloader = mock[DependencyDownloader]
        doReturn(Nil).when(mockDependencyDownloader).retrieve(
          anyString(), anyString(), anyString(), anyBoolean(), anyBoolean())
        val mockInterpreter = mock[Interpreter]

        val addDepsMagic = new AddDeps
          with IncludeSparkContext
          with IncludeInterpreter
          with IncludeOutputStream
          with IncludeDependencyDownloader
          with ArgumentParsingSupport
        {
          override val sparkContext: SparkContext = mock[SparkContext]
          override val interpreter: Interpreter = mockInterpreter
          override val dependencyDownloader: DependencyDownloader =
            mockDependencyDownloader
          override val outputStream: OutputStream = mock[OutputStream]
        }

        val expected = "org.apache.toree" :: "kernel" :: "1.0" :: Nil
        addDepsMagic.execute(expected.mkString(" "))

        verify(mockInterpreter).addJars(any[URL])
      }

      it("should add retrieved artifacts to the spark context") {
        val mockDependencyDownloader = mock[DependencyDownloader]
        val fakeUrl = new URL("file:/foo")
        doReturn(fakeUrl :: fakeUrl :: fakeUrl :: Nil)
          .when(mockDependencyDownloader).retrieve(
            anyString(), anyString(), anyString(), anyBoolean(), anyBoolean()
          )
        val mockSparkContext = mock[SparkContext]

        val addDepsMagic = new AddDeps
          with IncludeSparkContext
          with IncludeInterpreter
          with IncludeOutputStream
          with IncludeDependencyDownloader
          with ArgumentParsingSupport
        {
          override val sparkContext: SparkContext = mockSparkContext
          override val interpreter: Interpreter = mock[Interpreter]
          override val dependencyDownloader: DependencyDownloader =
            mockDependencyDownloader
          override val outputStream: OutputStream = mock[OutputStream]
        }

        val expected = "org.apache.toree" :: "kernel" :: "1.0" :: Nil
        addDepsMagic.execute(expected.mkString(" "))

        verify(mockSparkContext, times(3)).addJar(anyString())
      }
    }
  }
}
