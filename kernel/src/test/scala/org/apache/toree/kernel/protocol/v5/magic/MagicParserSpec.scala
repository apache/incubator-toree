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

package org.apache.toree.kernel.protocol.v5.magic

import org.apache.toree.magic.{CellMagic, Magic, MagicManager}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import org.mockito.Mockito._
import org.mockito.Matchers._

class MagicParserSpec extends FunSpec with Matchers with MockitoSugar {
  describe("#parse") {
    it("should call parseCell if the code is a cell magic invocation") {
      val codeBlob =
        """
          |%%magic
          |foo
          |bean
        """.stripMargin
      val parser = spy(new MagicParser(mock[MagicManager]))
      parser.parse(codeBlob)
      verify(parser).parseCell(codeBlob.trim)
    }

    it("should call parseLines if the code is not a cell magic") {
      val codeBlob = """%magic foo bean"""
      val parser = spy(new MagicParser(mock[MagicManager]))
      parser.parse(codeBlob)
      verify(parser).parseLines(codeBlob.trim)
    }
  }

  describe("#parseCell") {
    it("should substitute the magic code for kernel code when magic is valid") {
      val magicManager = mock[MagicManager]
      val mockMagic = mock[Magic]
      doReturn(mockMagic).when(magicManager).findMagic(anyString())
      doReturn(true).when(magicManager).isCellMagic(mockMagic)

      val magicName = "magic"
      val args = "foo\nbean\nbar"
      val codeBlob =
        s"""%%$magicName
           |$args
         """.stripMargin
      val parser = spy(new MagicParser(magicManager))
      val result = parser.parseCell(codeBlob)

      verify(parser).substitute(magicName, args)
      result.isLeft should be(true)
    }

    it("should return an error if the magic invocation is invalid") {
      val magicManager = mock[MagicManager]
      val mockMagic = mock[Magic]
      doReturn(mockMagic).when(magicManager).findMagic(anyString())
      doReturn(false).when(magicManager).isCellMagic(mockMagic)

      val magicName = "magic"
      val args = "foo\nbean\nbar"
      val codeBlob =
        s"""%%$magicName
           |$args
         """.stripMargin
      val parser = spy(new MagicParser(magicManager))
      val result = parser.parseCell(codeBlob)

      verify(parser, times(0)).substitute(anyString(), anyString())
      result.isRight should be(true)
    }

    it("should return original code if code contains no magic invocations") {
      val magicManager = mock[MagicManager]
      val mockMagic = mock[Magic]
      doReturn(mockMagic).when(magicManager).findMagic(anyString())
      doReturn(false).when(magicManager).isCellMagic(mockMagic)

      val codeBlob =
        s"""val x = 3
           |println(x + 2)
         """.stripMargin
      val parser = spy(new MagicParser(magicManager))
      val result = parser.parseCell(codeBlob)

      verify(parser, times(0)).substitute(anyString(), anyString())
      result.isLeft should be(true)
      result.left.get should be(codeBlob)
    }
  }

  describe("#parseLines") {
    it("should call substituteLine for each line of code " +
      "when there are no invalid magic invocations") {
      val magicManager = mock[MagicManager]
      val mockMagic = mock[Magic]
      doReturn(mockMagic).when(magicManager).findMagic(anyString())
      doReturn(true).when(magicManager).isLineMagic(mockMagic)

      val codeBlob =
        s"""val x = 3
           |%lineMagic
         """.stripMargin
      val parser = spy(new MagicParser(magicManager))
      val result = parser.parseLines(codeBlob)

      verify(parser, times(2)).substituteLine(anyString())
      result.isLeft should be(true)
    }

    it("should return an error when there are invalid magic invocations") {
      val magicManager = mock[MagicManager]
      val mockMagic = mock[Magic]
      doReturn(mockMagic).when(magicManager).findMagic(anyString())
      doReturn(false).when(magicManager).isLineMagic(mockMagic)

      val codeBlob =
        s"""val x = 3
           |%lineMagic
         """.stripMargin
      val parser = spy(new MagicParser(magicManager))
      val result = parser.parseLines(codeBlob)

      verify(parser, times(0)).substituteLine(anyString())
      result.isRight should be(true)
    }

    it("should return original code when there are no magic invocations") {
      val magicManager = mock[MagicManager]
      val mockMagic = mock[Magic]
      doReturn(mockMagic).when(magicManager).findMagic(anyString())
      doReturn(false).when(magicManager).isLineMagic(mockMagic)

      val codeBlob =
        s"""val x = 3
           |val y = x + 2
         """.stripMargin
      val parser = spy(new MagicParser(magicManager))
      val result = parser.parseLines(codeBlob.trim)

      result.isLeft should be(true)
      result.left.get should be(codeBlob.trim)
    }
  }

  describe("#parseMagic") {
    it("should extract the cell name and arguments from a valid invocation") {
      val magicName = "foobar"
      val magicArgs = "baz\nbean"
      val codeBlob = s"""%%$magicName\n$magicArgs"""
      val parser = new MagicParser(mock[MagicManager])
      parser.parseMagic(codeBlob) should be(Some((magicName, magicArgs)))
    }

    it("should extract the line name and arguments from a valid invocation") {
      val magicName = "foobar"
      val magicArgs = "baz\nbean"
      val codeBlob = s"""%$magicName $magicArgs"""
      val parser = new MagicParser(mock[MagicManager])
      parser.parseMagic(codeBlob) should be(Some((magicName, magicArgs)))
    }

    it("should return none if the invocation was not valid") {
      val magicName = "foobar"
      val magicArgs = "baz\nbean"
      val codeBlob = s"""$magicName\n$magicArgs"""
      val parser = new MagicParser(mock[MagicManager])
      parser.parseMagic(codeBlob) should be(None)
    }
  }

  describe("#substituteLine") {
    it("should call substitute when a codeLine is a valid magic invocation") {
      val magicName = "magic"
      val args = "-v foo bar"
      val codeLine = s"""%$magicName $args"""
      val parser = spy(new MagicParser(mock[MagicManager]))
      doReturn(true).when(parser).isValidLineMagic(anyString())
      parser.substituteLine(codeLine)
      verify(parser).substitute(magicName, args)
    }

    it("should return original line of code when it's not a valid +" +
      "magic invocation") {
      val codeLine = """val x = 3"""
      val parser = spy(new MagicParser(mock[MagicManager]))
      doReturn(false).when(parser).isValidLineMagic(anyString())
      parser.substituteLine(codeLine) should be(codeLine)
    }
  }

  describe("#substitute") {
    // pointless?
    it("should replace a magic invocation with an equivalent kernel call") {
      val magicName = "magic"
      val args = "foo bean"
      val parser = new MagicParser(mock[MagicManager])

      val equivalent =
        s"""${parser.kernelObjectName}.$magicName(\"\"\"$args\"\"\")"""
      parser.substitute(magicName, args) should be(equivalent)
    }
  }

  describe("#parseOutInvalidMagics") {
    it("it should return the names of invalid magics") {
      val magicOne = "foo"
      val magicTwo = "qux"
      val codeBlob =
        s"""
          |%$magicOne bar baz
          |%$magicTwo quo bean
        """.stripMargin
      val parser = spy(new MagicParser(mock[MagicManager]))
      doReturn(false).when(parser).isValidLineMagic(anyString())

      parser.parseOutInvalidMagics(codeBlob) should be(List(magicOne, magicTwo))
    }

    it("it should nothing if all magic invocations are valid") {
      val magicOne = "foo"
      val magicTwo = "qux"
      val codeBlob =
        s"""
          |%$magicOne bar baz
          |%$magicTwo quo bean
        """.stripMargin
      val parser = spy(new MagicParser(mock[MagicManager]))
      doReturn(true).when(parser).isValidLineMagic(anyString())

      parser.parseOutInvalidMagics(codeBlob) should be(Nil)
    }
  }

  describe("#isValidLineMagic") {
    it("should return true if the line magic invocation is valid") {
      val magicManager = mock[MagicManager]
      val mockMagic = mock[Magic]
      doReturn(mockMagic).when(magicManager).findMagic(anyString())
      doReturn(true).when(magicManager).isLineMagic(mockMagic)

      val parser = new MagicParser(magicManager)
      parser.isValidLineMagic("%foobar baz") should be(true)
    }

    it("should return false if the line magic invocation is not valid") {
      val magicManager = mock[MagicManager]
      val mockMagic = mock[Magic]
      doReturn(mockMagic).when(magicManager).findMagic(anyString())
      doReturn(false).when(magicManager).isLineMagic(mockMagic)

      val parser = new MagicParser(magicManager)
      parser.isValidLineMagic("%foobar baz") should be(false)
    }
  }
}
