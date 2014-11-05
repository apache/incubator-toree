package com.ibm.spark.kernel.protocol.v5.content

import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._
import com.ibm.spark.kernel.protocol.v5.Data

class CommOpenSpec extends FunSpec with Matchers {
  val commOpenJson: JsValue = Json.parse("""
  {
    "comm_id": "<UUID>",
    "target_name": "<STRING>",
    "data": {}
  }
  """)

  val commOpen = CommOpen(
    "<UUID>", "<STRING>", Data()
  )

  describe("CommOpen") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a CommOpen instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        commOpenJson.as[CommOpen] should be (commOpen)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newCompleteRequest = commOpenJson.asOpt[CommOpen]

        newCompleteRequest.get should be (commOpen)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val CompleteRequestResults = commOpenJson.validate[CommOpen]

        CompleteRequestResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: CommOpen) => valid
        ) should be (commOpen)
      }

      it("should implicitly convert from a CommOpen instance to valid json") {
        Json.toJson(commOpen) should be (commOpenJson)
      }
    }
  }
}

