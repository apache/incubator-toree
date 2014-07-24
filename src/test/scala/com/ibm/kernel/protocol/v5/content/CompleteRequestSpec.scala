package com.ibm.kernel.protocol.v5.content

import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class CompleteRequestSpec extends FunSpec with Matchers {
  val completeRequestJson: JsValue = Json.parse("""
  {
    "code": "<STRING>",
    "cursor_pos": 999
  }
  """)

  val completeRequest: CompleteRequest = CompleteRequest(
    "<STRING>", 999
  )

  describe("CompleteRequest") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a CompleteRequest instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        completeRequestJson.as[CompleteRequest] should be (completeRequest)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newCompleteRequest = completeRequestJson.asOpt[CompleteRequest]

        newCompleteRequest.get should be (completeRequest)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val CompleteRequestResults = completeRequestJson.validate[CompleteRequest]

        CompleteRequestResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: CompleteRequest) => valid
        ) should be (completeRequest)
      }

      it("should implicitly convert from a CompleteRequest instance to valid json") {
        Json.toJson(completeRequest) should be (completeRequestJson)
      }
    }
  }
}

