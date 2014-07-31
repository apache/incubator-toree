package com.ibm.spark.kernel.protocol.v5.content

import com.ibm.spark.kernel.protocol.v5._
import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class ExecuteReplySpec extends FunSpec with Matchers {
  val executeReplyJson: JsValue = Json.parse("""
  {
    "status": "<STRING>",
    "execution_count": 999
  }
  """)

  val executeReply: ExecuteReply = ExecuteReply(
    "<STRING>", 999, None, None, None, None, None
  )

  describe("ExecuteReply") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a executeReply instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        executeReplyJson.as[ExecuteReply] should be (executeReply)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newExecuteReply = executeReplyJson.asOpt[ExecuteReply]

        newExecuteReply.get should be (executeReply)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val executeReplyResults = executeReplyJson.validate[ExecuteReply]

        executeReplyResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: ExecuteReply) => valid
        ) should be (executeReply)
      }

      it("should implicitly convert from a executeReply instance to valid json") {
        Json.toJson(executeReply) should be (executeReplyJson)
      }
    }
  }
}

