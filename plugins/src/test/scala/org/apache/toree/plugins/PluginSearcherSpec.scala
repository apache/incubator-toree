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
package org.apache.toree.plugins

import java.io.File

import org.clapper.classutil.{Modifier, ClassFinder}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{OneInstancePerTest, Matchers, FunSpec}

import org.mockito.Mockito._
import test.utils.TestClassInfo

class PluginSearcherSpec extends FunSpec with Matchers
  with OneInstancePerTest with MockitoSugar
{
  private val mockClassFinder = mock[ClassFinder]
  private val pluginSearcher = new PluginSearcher {
    override protected def newClassFinder(): ClassFinder = mockClassFinder
    override protected def newClassFinder(paths: Seq[File]): ClassFinder =
      mockClassFinder
  }

  private val pluginClassInfo = TestClassInfo(
    name = classOf[Plugin].getName,
    modifiers = Set(Modifier.Interface)
  )
  private val directPluginClassInfo = TestClassInfo(
    name = "direct.plugin",
    superClassName = pluginClassInfo.name
  )
  private val directAsInterfacePluginClassInfo = TestClassInfo(
    name = "direct.interface.plugin",
    interfaces = List(pluginClassInfo.name)
  )
  private val indirectPluginClassInfo = TestClassInfo(
    name = "indirect.plugin",
    superClassName = directPluginClassInfo.name
  )
  private val indirectAsInterfacePluginClassInfo = TestClassInfo(
    name = "indirect.interface.plugin",
    interfaces = List(directAsInterfacePluginClassInfo.name)
  )
  private val traitPluginClassInfo = TestClassInfo(
    name = "trait.plugin",
    modifiers = Set(Modifier.Interface)
  )
  private val abstractClassPluginClassInfo = TestClassInfo(
    name = "abstract.plugin",
    modifiers = Set(Modifier.Abstract)
  )
  private val classInfos = Seq(
    pluginClassInfo,
    directPluginClassInfo, directAsInterfacePluginClassInfo,
    indirectPluginClassInfo, indirectAsInterfacePluginClassInfo,
    traitPluginClassInfo, abstractClassPluginClassInfo
  )

  describe("PluginSearcher") {
    describe("#internal") {
      it("should find any plugins directly extending the Plugin class") {
        val expected = directPluginClassInfo.name

        doReturn(classInfos.toStream).when(mockClassFinder).getClasses()

        val actual = pluginSearcher.internal.map(_.name)

        actual should contain (expected)
      }

      it("should find any plugins directly extending the Plugin trait") {
        val expected = directAsInterfacePluginClassInfo.name

        doReturn(classInfos.toStream).when(mockClassFinder).getClasses()

        val actual = pluginSearcher.internal.map(_.name)

        actual should contain (expected)
      }

      it("should find any plugins indirectly extending the Plugin class") {
        val expected = indirectPluginClassInfo.name

        doReturn(classInfos.toStream).when(mockClassFinder).getClasses()

        val actual = pluginSearcher.internal.map(_.name)

        actual should contain (expected)
      }

      it("should find any plugins indirectly extending the Plugin trait") {
        val expected = indirectAsInterfacePluginClassInfo.name

        doReturn(classInfos.toStream).when(mockClassFinder).getClasses()

        val actual = pluginSearcher.internal.map(_.name)

        actual should contain (expected)
      }

      it("should not include any traits or abstract classes") {
        val expected = Seq(
          abstractClassPluginClassInfo.name,
          traitPluginClassInfo.name
        )

        doReturn(classInfos.toStream).when(mockClassFinder).getClasses()

        val actual = pluginSearcher.internal.map(_.name)

        actual should not contain atLeastOneOf (expected.head, expected.tail)
      }
    }

    describe("#search") {
      it("should find any plugins directly extending the Plugin class") {
        val expected = directPluginClassInfo.name

        doReturn(classInfos.toStream).when(mockClassFinder).getClasses()

        val actual = pluginSearcher.search().map(_.name).toSeq

        actual should contain (expected)
      }

      it("should find any plugins directly extending the Plugin trait") {
        val expected = directAsInterfacePluginClassInfo.name

        doReturn(classInfos.toStream).when(mockClassFinder).getClasses()

        val actual = pluginSearcher.search().map(_.name).toSeq

        actual should contain (expected)
      }

      it("should find any plugins indirectly extending the Plugin class") {
        val expected = indirectPluginClassInfo.name

        doReturn(classInfos.toStream).when(mockClassFinder).getClasses()

        val actual = pluginSearcher.search().map(_.name).toSeq

        actual should contain (expected)
      }

      it("should find any plugins indirectly extending the Plugin trait") {
        val expected = indirectAsInterfacePluginClassInfo.name

        doReturn(classInfos.toStream).when(mockClassFinder).getClasses()

        val actual = pluginSearcher.search().map(_.name).toSeq

        actual should contain (expected)
      }

      it("should not include any traits or abstract classes") {
        val expected = Seq(
          abstractClassPluginClassInfo.name,
          traitPluginClassInfo.name
        )

        doReturn(classInfos.toStream).when(mockClassFinder).getClasses()

        val actual = pluginSearcher.search().map(_.name).toSeq

        actual should not contain atLeastOneOf (expected.head, expected.tail)
      }
    }
  }
}
