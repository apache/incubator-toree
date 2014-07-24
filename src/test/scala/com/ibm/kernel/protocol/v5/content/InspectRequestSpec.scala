package com.ibm.kernel.protocol.v5.content

import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class InspectRequestSpec extends FunSpec with Matchers {
  val inspectRequestJson: JsValue = Json.parse("""
  {
    "code": "<STRING>",
    "cursor_pos": 999,
    "detail_level": 1
  }
  """)

  val inspectRequest: InspectRequest = InspectRequest(
    "<STRING>", 999, 1
  )

  describe("InspectRequest") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a InspectRequest instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        inspectRequestJson.as[InspectRequest] should be (inspectRequest)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newInspectRequest = inspectRequestJson.asOpt[InspectRequest]

        newInspectRequest.get should be (inspectRequest)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val InspectRequestResults = inspectRequestJson.validate[InspectRequest]

        InspectRequestResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: InspectRequest) => valid
        ) should be (inspectRequest)
      }

      it("should implicitly convert from a InspectRequest instance to valid json") {
        Json.toJson(inspectRequest) should be (inspectRequestJson)
      }
    }
  }
}

