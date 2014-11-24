package com.ibm.spark.magic.builtin

import com.ibm.spark.interpreter.Interpreter
import com.ibm.spark.interpreter.Results.Result
import com.ibm.spark.kernel.protocol.v5.MIMEType
import com.ibm.spark.magic.dependencies.IncludeInterpreter
import org.scalatest.{BeforeAndAfter, Matchers, FunSpec}
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._

class PlotSpec extends FunSpec with Matchers with MockitoSugar
  with BeforeAndAfter {

  val mockInterpreter = mock[Interpreter]
  val image = "image"
  val plotMagic = new Plot with IncludeInterpreter {
    override val interpreter: Interpreter = mockInterpreter
  }

  before {
    doReturn(Option(image)).when(mockInterpreter).read(anyString())
    doReturn((mock[Result], Left(""))).when(mockInterpreter)
      .interpret(anyString(), anyBoolean())
  }

  def plotCell(code: Seq[String]) =
    assertResult(image, "plot magic should return an image") {
      val magicOutput = plotMagic.executeCell(code)
      assert(magicOutput.contains(MIMEType.ImagePng),
        "plot magic result should contain an image")
      magicOutput(MIMEType.ImagePng)
    }

  def plotLine(code: String) =
    assertResult(image, "plot magic should return an image") {
      val magicOutput = plotMagic.executeLine(code)
      assert(magicOutput.contains(MIMEType.ImagePng),
        "plot magic result should contain an image")
      magicOutput(MIMEType.ImagePng)
    }

  describe("plot") {
    describe("#executeLine") {
      it("should return image/png with correct arguments") {
        plotLine("--width=100 --height=100 --chart=chart")
      }
      it("should return image/png without width or height") {
        plotLine("--chart=chart")
      }
      it("should return image/png with width only") {
        plotLine("--width=100 --chart=chart")
      }
      it("should throw error when missing chart argument") {
        intercept[NullPointerException] {
          plotMagic.executeLine("foo")
        }
      }
      it("should return text/plain when error in code") {
        doReturn((mock[Result], Right("error"))).when(mockInterpreter)
          .interpret(anyString(), anyBoolean())
        assertResult("error", "plot magic should return a plain text error message") {
          val magicOutput = plotMagic.executeLine("--chart=chart")
          assert(magicOutput.contains(MIMEType.PlainText),
            "plot magic result should contain a text error message")
          magicOutput(MIMEType.PlainText)
        }
      }
    }
  }
}