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

package org.apache.toree.magic

import org.apache.toree.plugins.dependencies.Dependency
import org.apache.toree.plugins._
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => mockEq, _}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers, OneInstancePerTest}
import test.utils

import MagicManagerSpec._

import scala.runtime.BoxedUnit

object MagicManagerSpec {
  val TestCellMagicOutput = CellMagicOutput("test" -> "value")
}

class SomeLineMagic extends LineMagic {
  override def execute(code: String): Unit = {}
}

class SomeCellMagic extends CellMagic {
  override def execute(code: String): CellMagicOutput = TestCellMagicOutput
}

private class SomePlugin extends Plugin

private class SomeMagic extends Magic {
  override def execute(code: String): Any = ???
}

class LineMagicException extends Exception

private class ExceptionLineMagic extends LineMagic {
  override def execute(code: String): Unit = throw new LineMagicException
}

class CellMagicException extends Exception

private class ExceptionCellMagic extends CellMagic {
  override def execute(code: String): CellMagicOutput = throw new CellMagicException
}

class MagicManagerSpec
  extends FunSpec with Matchers with MockitoSugar with OneInstancePerTest
{
  private val TestPluginName = "SomePlugin"
  private val TestMagicName = "SomeMagic"
  private val mockPluginManager = mock[PluginManager]
  private val magicManager = spy(new MagicManager(mockPluginManager))

  describe("MagicManager") {
    describe("#isLineMagic") {
      it("should return true if the magic extends the line magic interface") {
        val expected = true

        val mockLineMagic = mock[LineMagic]
        val actual = magicManager.isLineMagic(mockLineMagic)

        actual should be (expected)
      }

      it("should return false if the magic does not extend the line magic interface") {
        val expected = false

        val mockMagic = mock[Magic]
        val actual = magicManager.isLineMagic(mockMagic)

        actual should be (expected)
      }

      it("should throw an exception if provided null") {
        intercept[NullPointerException] {
          magicManager.isLineMagic(null)
        }
      }
    }

    describe("#isCellMagic") {
      it("should return true if the magic extends the cell magic interface") {
        val expected = true

        val mockCellMagic = mock[CellMagic]
        val actual = magicManager.isCellMagic(mockCellMagic)

        actual should be (expected)
      }

      it("should return false if the magic does not extend the cell magic interface") {
        val expected = false

        val mockMagic = mock[Magic]
        val actual = magicManager.isCellMagic(mockMagic)

        actual should be (expected)
      }

      it("should throw an exception if provided null") {
        intercept[NullPointerException] {
          magicManager.isCellMagic(null)
        }
      }
    }

    describe("#findMagic") {
      it("should throw a MagicNotFoundException if no magic matches the name") {
        intercept[MagicNotFoundException] {
          doReturn(Seq(new Plugin {}).toIterable).when(mockPluginManager).plugins

          magicManager.findMagic(TestMagicName)
        }
      }

      it("should throw a MagicNotFoundException if there are no loaded plugins") {
        intercept[MagicNotFoundException] {
          doReturn(Nil).when(mockPluginManager).plugins

          magicManager.findMagic(TestMagicName)
        }
      }

      it("should throw a MagicNotFoundException if a plugin matches but is not a magic") {
        intercept[MagicNotFoundException] {
          doReturn(Seq(new SomePlugin).toIterable).when(mockPluginManager).plugins

          magicManager.findMagic(TestPluginName)
        }
      }

      it("should return the magic if exactly one is found") {
        val expected = new SomeMagic

        doReturn(Seq(expected).toIterable).when(mockPluginManager).plugins
        val actual = magicManager.findMagic(TestMagicName)

        actual should be (expected)
      }

      it("should return a magic whose name matches even if casing is different") {
        val expected = new SomeMagic

        doReturn(Seq(expected).toIterable).when(mockPluginManager).plugins
        val actual = magicManager.findMagic(TestMagicName.toUpperCase())

        actual should be (expected)
      }

      it("should return the first match if more than one magic matches the name") {
        val expected = new SomeMagic

        doReturn(Seq(expected, new utils.SomeMagic).toIterable)
          .when(mockPluginManager).plugins
        val actual = magicManager.findMagic(TestMagicName)

        actual should be (expected)
      }
    }

    describe("#applyDynamic") {
      it("should return CellMagicOutput if the invocation of a magic throws an exception") {
        doReturn(Some(FailurePluginMethodResult(
          mock[PluginMethod],
          new LineMagicException()
        ))).when(mockPluginManager).fireEventFirstResult(
          anyString(), any(classOf[Dependency[_ <: AnyRef]])
        )

        val result = magicManager.applyDynamic("TEST")()

        result.asMap.get("text/plain") should not be(empty)
      }

      it("should fire an event with the lowercase of the magic name") {
        val arg: java.lang.String = "some arg"
        val pluginName = "TEST"
        val expected = Dependency.fromValueWithName("input", arg)

        doReturn(Some(FailurePluginMethodResult(
          mock[PluginMethod],
          new LineMagicException()
        ))).when(mockPluginManager).fireEventFirstResult(
          anyString(), any(classOf[Dependency[_ <: AnyRef]])
        )

        magicManager.applyDynamic(pluginName)(arg :: Nil: _*)
        verify(mockPluginManager).fireEventFirstResult(mockEq(pluginName.toLowerCase), any())
      }

      it("should take the first argument and convert it to a string to pass to the magic") {
        val arg: java.lang.String = "some arg"
        val pluginName = "TEST"
        val expected = Dependency.fromValueWithName("input", arg)

        doReturn(Some(FailurePluginMethodResult(
          mock[PluginMethod],
          new LineMagicException()
        ))).when(mockPluginManager).fireEventFirstResult(
          anyString(), any(classOf[Dependency[_ <: AnyRef]])
        )

        magicManager.applyDynamic(pluginName)(arg :: Nil: _*)
        verify(mockPluginManager).fireEventFirstResult(anyString(), mockEq(Seq(expected)): _*)
      }

      it("should pass an empty string to the line magic if no arguments are provided") {
        val arg: java.lang.String = ""
        val pluginName = "TEST"
        val expected = Dependency.fromValueWithName("input", arg)

        doReturn(Some(FailurePluginMethodResult(
          mock[PluginMethod],
          new LineMagicException()
        ))).when(mockPluginManager).fireEventFirstResult(
          anyString(), any(classOf[Dependency[_ <: AnyRef]])
        )

        magicManager.applyDynamic(pluginName)(Nil: _*)
        verify(mockPluginManager).fireEventFirstResult(anyString(), mockEq(Seq(expected)): _*)
      }

      it("should return a Right[LineMagicOutput] if line magic execution is successful and returns null") {
        val pluginName = "TEST"
        val expected = LineMagicOutput

        doReturn(Some(SuccessPluginMethodResult(
          mock[PluginMethod],
          null
        ))).when(mockPluginManager).fireEventFirstResult(
          anyString(), any(classOf[Dependency[_ <: AnyRef]])
        )

        val result = magicManager.applyDynamic(pluginName)(Nil: _*)
        result should be(expected)
      }

      it("should return a Right[LineMagicOutput] if line magic execution is successful and returns BoxedUnit") {
        val pluginName = "TEST"
        val expected = LineMagicOutput

        doReturn(Some(SuccessPluginMethodResult(
          mock[PluginMethod],
          BoxedUnit.UNIT
        ))).when(mockPluginManager).fireEventFirstResult(
          anyString(), any(classOf[Dependency[_ <: AnyRef]])
        )

        val result = magicManager.applyDynamic(pluginName)(Nil: _*)
        result should be(expected)
      }

      it("should return a Left[CellMagicOutput] if cell magic execution is successful") {
        val pluginName = "TEST"
        val cellMagicOutput = CellMagicOutput("our/type" -> "TEST CONTENT")
        doReturn(Some(SuccessPluginMethodResult(
          mock[PluginMethod],
          cellMagicOutput
        ))).when(mockPluginManager).fireEventFirstResult(
          anyString(), any(classOf[Dependency[_ <: AnyRef]])
        )

        val result = magicManager.applyDynamic(pluginName)(Nil: _*)
        result should be(cellMagicOutput)
      }

      it("should return a Left[CellMagicOutput] if is a magic but not a line or cell") {
        val pluginName = "TEST"

        doReturn(Some(SuccessPluginMethodResult(
          mock[PluginMethod],
          new AnyRef
        ))).when(mockPluginManager).fireEventFirstResult(
          anyString(), any(classOf[Dependency[_ <: AnyRef]])
        )

        val result = magicManager.applyDynamic(pluginName)(Nil: _*)
        result.asMap.get("text/plain") should not be (empty)

      }

      it("should return a Left[CellMagicOutput] if magic fails") {
        val pluginName = "TEST"

        doReturn(Some(FailurePluginMethodResult(
          mock[PluginMethod],
          new Throwable
        ))).when(mockPluginManager).fireEventFirstResult(
          anyString(), any(classOf[Dependency[_ <: AnyRef]])
        )

        val result = magicManager.applyDynamic(pluginName)(Nil: _*)
        result.asMap.get("text/plain") should not be (empty)
      }

      it("should throw a MagicNotFoundException when a magic cannot be found") {
        val pluginName = "THISMAGICDOESN'TEXIST"

        doReturn(None).when(mockPluginManager).fireEventFirstResult(
          anyString(), any(classOf[Dependency[_ <: AnyRef]])
        )
        intercept[MagicNotFoundException] {
          magicManager.applyDynamic(pluginName)(Nil: _*)
        }
      }
    }
  }
}
