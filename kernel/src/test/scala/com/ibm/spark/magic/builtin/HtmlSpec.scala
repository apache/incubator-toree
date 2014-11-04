package com.ibm.spark.magic.builtin

import com.ibm.spark.kernel.protocol.v5.MIMEType
import com.ibm.spark.magic.MagicOutput
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

class HtmlSpec extends FunSpec with Matchers with MockitoSugar {
  describe("Html"){
    describe("#executeCell") {
      it("should return the entire cell's contents with the MIME type of text/html") {
        val htmlMagic = new Html

        val code = "some code on a line" :: "more code on another line" :: Nil
        val expected = MagicOutput(MIMEType.TextHtml -> code.mkString("\n"))
        htmlMagic.executeCell(code) should be (expected)
      }
    }

    describe("#executeLine") {
      it("should return the line's contents with the MIME type of text/html") {
        val htmlMagic = new Html

        val code = "some code on a line"
        val expected = MagicOutput(MIMEType.TextHtml -> code)
        htmlMagic.executeLine(code) should be (expected)
      }
    }
  }
}
