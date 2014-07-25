package com.ibm.kernel.protocol.v5.content

import org.scalatest.FunSuite

import org.scalatest.{Matchers, FunSpec}
import play.api.data.validation.ValidationError
import play.api.libs.json._
import com.ibm.kernel.protocol.v5._

class CodeInputsSpec extends FunSpec with Matchers {
  val codeInputsJson: JsValue = Json.parse("""
  {
    "code": "<STRING>",
    "execution_count": 42
  }
  """)

  val codeInputs: CodeInputs = CodeInputs(
    "<STRING>", 42
  )

  describe("CodeInputs") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a codeInputs instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        codeInputsJson.as[CodeInputs] should be (codeInputs)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newCodeInputs = codeInputsJson.asOpt[CodeInputs]

        newCodeInputs.get should be (codeInputs)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val codeInputsResults = codeInputsJson.validate[CodeInputs]

        codeInputsResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: CodeInputs) => valid
        ) should be (codeInputs)
      }

      it("should implicitly convert from a codeInputs instance to valid json") {
        Json.toJson(codeInputs) should be (codeInputsJson)
      }
    }
  }
}
