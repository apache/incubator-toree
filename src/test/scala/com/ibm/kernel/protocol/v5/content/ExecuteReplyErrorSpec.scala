package com.ibm.kernel.protocol.v5.content

import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._

import com.ibm.kernel.protocol.v5._

class ExecuteReplyErrorSpec extends FunSpec with Matchers {
  val executeReplyErrorJson: JsValue = Json.parse("""
  {
    "status": "error",
    "execution_count": 999,
    "ename": "<STRING>",
    "evalue": "<STRING>",
    "traceback": []
  }
  """)

  val executeReplyError: ExecuteReplyError = ExecuteReplyError(
    999, Some("<STRING>"), Some("<STRING>"), Some(List())
  )

  describe("ExecuteReplyError") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a ExecuteReplyError instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        executeReplyErrorJson.as[ExecuteReplyError] should be (executeReplyError)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newExecuteReplyError = executeReplyErrorJson.asOpt[ExecuteReplyError]

        newExecuteReplyError.get should be (executeReplyError)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val ExecuteReplyErrorResults = executeReplyErrorJson.validate[ExecuteReplyError]

        ExecuteReplyErrorResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: ExecuteReplyError) => valid
        ) should be (executeReplyError)
      }

      it("should implicitly convert from a ExecuteReplyError instance to valid json") {
        Json.toJson(executeReplyError) should be (executeReplyErrorJson)
      }
    }
  }
}

