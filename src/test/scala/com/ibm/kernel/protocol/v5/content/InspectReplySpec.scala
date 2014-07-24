package com.ibm.kernel.protocol.v5.content

import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class InspectReplySpec extends FunSpec with Matchers {
  val inspectReplyJson: JsValue = Json.parse("""
  {
    "status": "<STRING>",
    "data": {},
    "metadata": {}
  }
  """)

  val inspectReply: InspectReply = InspectReply(
    "<STRING>", Map(), Map(), None, None, None
  )

  describe("InspectReply") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a InspectReply instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        inspectReplyJson.as[InspectReply] should be (inspectReply)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newInspectReply = inspectReplyJson.asOpt[InspectReply]

        newInspectReply.get should be (inspectReply)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val InspectReplyResults = inspectReplyJson.validate[InspectReply]

        InspectReplyResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: InspectReply) => valid
        ) should be (inspectReply)
      }

      it("should implicitly convert from a InspectReply instance to valid json") {
        Json.toJson(inspectReply) should be (inspectReplyJson)
      }
    }
  }
}

