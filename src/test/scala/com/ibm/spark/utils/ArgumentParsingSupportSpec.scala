package com.ibm.spark.utils

import org.scalatest.{BeforeAndAfter, Matchers, FunSpec}
import joptsimple.{OptionSet, OptionSpec, OptionParser}
import org.scalatest.mock.MockitoSugar

import org.mockito.Mockito._
import org.mockito.Matchers._

import collection.JavaConverters._

class ArgumentParsingSupportSpec extends FunSpec with Matchers
  with BeforeAndAfter with MockitoSugar
{
  private var mockOptions: OptionSet = _
  private var mockParser: OptionParser = _
  private var argumentParsingInstance: ArgumentParsingSupport = _

  before {
    mockOptions = mock[OptionSet]
    mockParser = mock[OptionParser]
    doReturn(mockOptions).when(mockParser).parse(anyVararg[String]())

    argumentParsingInstance = new Object() with ArgumentParsingSupport {
      override protected lazy val parser: OptionParser = mockParser
    }
  }

  describe("ArgumentParsingSupport") {
    describe("#parseArgs") {
      it("should invoke the underlying parser's parse method") {
        doReturn(Nil.asJava).when(mockOptions).nonOptionArguments()
        argumentParsingInstance.parseArgs("")

        verify(mockParser).parse(anyString())
      }

      it("should return an empty list if there are no non-option arguments") {
        val expected = Nil
        doReturn(expected.asJava).when(mockOptions).nonOptionArguments()
        val actual = argumentParsingInstance.parseArgs((
          "--transitive" :: expected
        ).mkString(" "))

        actual should be (expected)
      }

      it("should return a list containing non-option arguments") {
        val expected = "non-option" :: Nil
        doReturn(expected.asJava).when(mockOptions).nonOptionArguments()
        val actual = argumentParsingInstance.parseArgs((
          "--transitive" :: expected
          ).mkString(" "))

        actual should be (expected)
      }
    }
  }
}
