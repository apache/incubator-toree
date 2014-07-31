package com.ibm.spark.kernel.protocol.v5.content

import com.ibm.spark.kernel.protocol.v5._
import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class ExecuteReplyAbortSpec extends FunSpec with Matchers {
  val executeReplyAbortJson: JsValue = Json.parse("""
  {
    "status": "abort",
    "execution_count": 999
  }
  """)

  val executeReplyAbort: ExecuteReplyAbort = ExecuteReplyAbort(
    999
  )

  describe("ExecuteReplyAbort") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a ExecuteReplyAbort instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        executeReplyAbortJson.as[ExecuteReplyAbort] should be (executeReplyAbort)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newExecuteReplyAbort = executeReplyAbortJson.asOpt[ExecuteReplyAbort]

        newExecuteReplyAbort.get should be (executeReplyAbort)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val ExecuteReplyAbortResults = executeReplyAbortJson.validate[ExecuteReplyAbort]

        ExecuteReplyAbortResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: ExecuteReplyAbort) => valid
        ) should be (executeReplyAbort)
      }

      it("should implicitly convert from a ExecuteReplyAbort instance to valid json") {
        Json.toJson(executeReplyAbort) should be (executeReplyAbortJson)
      }
    }
  }
}

