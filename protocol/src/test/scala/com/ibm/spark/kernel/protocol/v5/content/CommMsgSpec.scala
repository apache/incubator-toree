package com.ibm.spark.kernel.protocol.v5.content

import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._
import com.ibm.spark.kernel.protocol.v5.Data

class CommMsgSpec extends FunSpec with Matchers {
  val commMsgJson: JsValue = Json.parse("""
  {
    "comm_id": "<UUID>",
    "data": {}
  }
  """)

  val commMsg = CommMsg(
    "<UUID>", Data()
  )

  describe("CommMsg") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a CommMsg instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        commMsgJson.as[CommMsg] should be (commMsg)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newCompleteRequest = commMsgJson.asOpt[CommMsg]

        newCompleteRequest.get should be (commMsg)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val CompleteRequestResults = commMsgJson.validate[CommMsg]

        CompleteRequestResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: CommMsg) => valid
        ) should be (commMsg)
      }

      it("should implicitly convert from a CommMsg instance to valid json") {
        Json.toJson(commMsg) should be (commMsgJson)
      }
    }
  }
}

