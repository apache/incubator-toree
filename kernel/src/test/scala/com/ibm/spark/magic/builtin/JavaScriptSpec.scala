package com.ibm.spark.magic.builtin

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import com.ibm.spark.magic.MagicOutput
import com.ibm.spark.kernel.protocol.v5.MIMEType

class JavaScriptSpec extends FunSpec with Matchers with MockitoSugar {
  describe("JavaScript"){
    describe("#executeCell") {
      it("should return the entire cell's contents with the MIME type of text/javascript") {
        val javaScriptMagic = new JavaScript

        val code = "some code on a line" :: "more code on another line" :: Nil
        val expected = MagicOutput(MIMEType.ApplicationJavaScript -> code.mkString("\n"))
        javaScriptMagic.executeCell(code) should be (expected)
      }
    }

    describe("#executeLine") {
      it("should return the line's contents with the MIME type of text/javascript") {
        val javaScriptMagic = new JavaScript

        val code = "some code on a line"
        val expected = MagicOutput(MIMEType.ApplicationJavaScript -> code)
        javaScriptMagic.executeLine(code) should be (expected)
      }
    }
  }
}
