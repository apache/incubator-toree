package com.ibm.kernel.protocol.v5.content


import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class ClearOutputSpec extends FunSpec with Matchers {
  val clearOutputJson: JsValue = Json.parse("""
  {
    "wait": true
  }
""")

  val clearOutput = ClearOutput(
    true
  )

  describe("ClearOutput") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a ClearOutput instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        clearOutputJson.as[ClearOutput] should be (clearOutput)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newCompleteRequest = clearOutputJson.asOpt[ClearOutput]

        newCompleteRequest.get should be (clearOutput)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val CompleteRequestResults = clearOutputJson.validate[ClearOutput]

        CompleteRequestResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: ClearOutput) => valid
        ) should be (clearOutput)
      }

      it("should implicitly convert from a ClearOutput instance to valid json") {
        Json.toJson(clearOutput) should be (clearOutputJson)
      }
    }
  }
}

