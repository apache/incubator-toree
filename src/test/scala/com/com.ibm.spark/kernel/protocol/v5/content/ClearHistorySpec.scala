package com.ibm.kernel.protocol.v5.content


import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class ClearHistorySpec extends FunSpec with Matchers {
  val clearHistoryJson: JsValue = Json.parse("""
  {
    "matches": true
  }
""")

  val clearHistory = ClearHistory(
    true
  )

  describe("ClearHistory") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a ClearHistory instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        clearHistoryJson.as[ClearHistory] should be (clearHistory)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newCompleteRequest = clearHistoryJson.asOpt[ClearHistory]

        newCompleteRequest.get should be (clearHistory)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val CompleteRequestResults = clearHistoryJson.validate[ClearHistory]

        CompleteRequestResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: ClearHistory) => valid
        ) should be (clearHistory)
      }

      it("should implicitly convert from a ClearHistory instance to valid json") {
        Json.toJson(clearHistory) should be (clearHistoryJson)
      }
    }
  }
}

