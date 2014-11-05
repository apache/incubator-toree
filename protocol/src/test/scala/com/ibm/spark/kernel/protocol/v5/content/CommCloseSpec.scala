package com.ibm.spark.kernel.protocol.v5.content

import com.ibm.spark.kernel.protocol.v5.Data
import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class CommCloseSpec extends FunSpec with Matchers {
  val commCloseJson: JsValue = Json.parse("""
  {
    "comm_id": "<UUID>",
    "data": {}
  }
  """)

  val commClose = CommClose(
    "<UUID>", Data()
  )

  describe("CommClose") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a CommClose instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        commCloseJson.as[CommClose] should be (commClose)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newCompleteRequest = commCloseJson.asOpt[CommClose]

        newCompleteRequest.get should be (commClose)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val CompleteRequestResults = commCloseJson.validate[CommClose]

        CompleteRequestResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: CommClose) => valid
        ) should be (commClose)
      }

      it("should implicitly convert from a CommClose instance to valid json") {
        Json.toJson(commClose) should be (commCloseJson)
      }
    }
  }
}

