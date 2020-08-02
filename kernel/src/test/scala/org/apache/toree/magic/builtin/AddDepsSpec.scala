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

import java.io.{ByteArrayOutputStream, OutputStream}
import java.net.{URI, URL}
import org.apache.toree.dependencies.{Credentials, DependencyDownloader}
import org.apache.toree.utils.ArgumentParsingSupport
import org.apache.toree.kernel.api.KernelLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpec, GivenWhenThen, Matchers}
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
        val mockKernel = mock[KernelLike]
        val mockDownloader = mock[DependencyDownloader]
        var printHelpWasRun = false

        val addDepsMagic = new AddDeps
          with IncludeKernel
          with IncludeOutputStream
          with IncludeDependencyDownloader
          with ArgumentParsingSupport
        {
          override val kernel: KernelLike = mockKernel
          override val dependencyDownloader: DependencyDownloader =
            mockDownloader
          override val outputStream: OutputStream = byteArrayOutputStream

          override def printHelp(
            outputStream: OutputStream, usage: String
          ): Unit = printHelpWasRun = true
        }

        val expected = LineMagicOutput
        addDepsMagic.execute("notvalid")

        printHelpWasRun should be (true)
        verify(mockKernel, times(0)).addJars(any())
        verify(mockDownloader, times(0)).retrieve(
          anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(),
          anyBoolean(), any[Seq[(URL, Option[Credentials])]], anyBoolean(), anyBoolean(),
          any[Option[String]], any[Option[String]], any[Option[String]], any[Set[(String,String)]]
        )
      }

      it("should set the retrievals transitive to true if provided") {
        val mockDependencyDownloader = mock[DependencyDownloader]
        doReturn(Nil).when(mockDependencyDownloader).retrieve(
          anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(),
          anyBoolean(), any[Seq[(URL, Option[Credentials])]], anyBoolean(), anyBoolean(),
          any[Option[String]], any[Option[String]], any[Option[String]], any[Set[(String,String)]]
        )

        val addDepsMagic = new AddDeps
          with IncludeKernel
          with IncludeOutputStream
          with IncludeDependencyDownloader
          with ArgumentParsingSupport
        {
          override val kernel: KernelLike = mock[KernelLike]
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
          anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(),
          anyBoolean(), any[Seq[(URL, Option[Credentials])]], anyBoolean(), anyBoolean(),
          any[Option[String]], any[Option[String]], any[Option[String]], any[Set[(String,String)]]
        )

        val addDepsMagic = new AddDeps
          with IncludeKernel
          with IncludeOutputStream
          with IncludeDependencyDownloader
          with ArgumentParsingSupport
        {
          override val kernel: KernelLike = mock[KernelLike]
          override val dependencyDownloader: DependencyDownloader =
            mockDependencyDownloader
          override val outputStream: OutputStream = mock[OutputStream]
        }

        val expected = "org.apache.toree" :: "kernel" :: "1.0" :: Nil
        addDepsMagic.execute(expected.mkString(" "))

        verify(mockDependencyDownloader).retrieve(
          expected(0), expected(1), expected(2), false)
      }

      it("should add retrieved artifacts to the kernel") {
        val mockDependencyDownloader = mock[DependencyDownloader]
        doReturn(Nil).when(mockDependencyDownloader).retrieve(
          anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(),
          anyBoolean(), any[Seq[(URL, Option[Credentials])]], anyBoolean(), anyBoolean(),
          any[Option[String]], any[Option[String]], any[Option[String]], any[Set[(String,String)]]
        )
        val mockKernel = mock[KernelLike]

        val addDepsMagic = new AddDeps
          with IncludeKernel
          with IncludeOutputStream
          with IncludeDependencyDownloader
          with ArgumentParsingSupport
        {
          override val kernel: KernelLike = mockKernel
          override val dependencyDownloader: DependencyDownloader =
            mockDependencyDownloader
          override val outputStream: OutputStream = mock[OutputStream]
        }

        val expected = "org.apache.toree" :: "kernel" :: "1.0" :: Nil
        addDepsMagic.execute(expected.mkString(" "))

        verify(mockKernel).addJars(any[URI])
      }
    }
  }
}
