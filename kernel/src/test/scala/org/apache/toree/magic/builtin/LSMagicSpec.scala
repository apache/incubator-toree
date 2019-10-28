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

import java.io.OutputStream
import java.net.URL

import org.apache.toree.interpreter.Interpreter
import org.apache.toree.magic.dependencies.{IncludeOutputStream, IncludeInterpreter}
import org.apache.toree.magic.{CellMagic, LineMagic}
import org.apache.spark.SparkContext
import org.scalatest.{Matchers, FunSpec}
import org.scalatestplus.mockito.MockitoSugar

import org.mockito.Mockito._
import org.mockito.Matchers._

class TestLSMagic(sc: SparkContext, intp: Interpreter, os: OutputStream)
  extends LSMagic
  with IncludeInterpreter
  with IncludeOutputStream
  {
    override val interpreter: Interpreter = intp
    override val outputStream: OutputStream = os
  }

class LSMagicSpec extends FunSpec with Matchers with MockitoSugar {
  describe("LSMagic") {

    describe("#execute") {
      it("should call println with a magics message") {
        val lsm = spy(new TestLSMagic(
          mock[SparkContext], mock[Interpreter], mock[OutputStream])
        )
        val classList = new BuiltinLoader().loadClasses()
        lsm.execute("")
        verify(lsm).magicNames("%", classOf[LineMagic], classList)
        verify(lsm).magicNames("%%", classOf[CellMagic], classList)
      }
    }

    describe("#magicNames") {
      it("should filter classnames by interface") {
        val prefix = "%"
        val interface = classOf[LineMagic]
        val classes : List[Class[_]] = List(classOf[LSMagic], classOf[Integer])
        val lsm = new TestLSMagic(
          mock[SparkContext], mock[Interpreter], mock[OutputStream])
        lsm.magicNames(prefix, interface, classes).length should be(1)
      }
      it("should prepend prefix to each name"){
        val prefix = "%"
        val className = classOf[LSMagic].getSimpleName
        val interface = classOf[LineMagic]
        val expected = s"${prefix}${className}"
        val classes : List[Class[_]] = List(classOf[LSMagic], classOf[Integer])
        val lsm = new TestLSMagic(
          mock[SparkContext], mock[Interpreter], mock[OutputStream])
        lsm.magicNames(prefix, interface, classes) should be(List(expected))
      }
    }

  }

}
