package com.ibm.kernel.protocol.v5.content

import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._

import com.ibm.kernel.protocol.v5._

class InspectReplyOkSpec extends FunSpec with Matchers {
  val inspectReplyOkJson: JsValue = Json.parse("""
  {
    "status": "ok",
    "data": {},
    "metadata": {}
  }
  """)

  val inspectReplyOk: InspectReplyOk = InspectReplyOk(
    Data(), Metadata()
  )

  describe("InspectReplyOk") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a InspectReplyOk instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        inspectReplyOkJson.as[InspectReplyOk] should be (inspectReplyOk)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newInspectReplyOk = inspectReplyOkJson.asOpt[InspectReplyOk]

        newInspectReplyOk.get should be (inspectReplyOk)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val InspectReplyOkResults = inspectReplyOkJson.validate[InspectReplyOk]

        InspectReplyOkResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: InspectReplyOk) => valid
        ) should be (inspectReplyOk)
      }

      it("should implicitly convert from a InspectReplyOk instance to valid json") {
        Json.toJson(inspectReplyOk) should be (inspectReplyOkJson)
      }
    }
  }
}

