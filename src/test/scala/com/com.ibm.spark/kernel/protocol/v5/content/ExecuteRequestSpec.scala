package com.ibm.kernel.protocol.v5.content

import org.scalatest.{Matchers, FunSpec}
import play.api.data.validation.ValidationError
import play.api.libs.json._
import com.ibm.kernel.protocol.v5._

class ExecuteRequestSpec extends FunSpec with Matchers {
  val executeRequestJson: JsValue = Json.parse("""
  {
    "code": "<STRING>",
    "silent": false,
    "store_history": false,
    "user_expressions": {},
    "allow_stdin": false
  }
  """)

  val executeRequest: ExecuteRequest = ExecuteRequest(
    "<STRING>", false, false, UserExpressions(), false
  )

  describe("ExecuteRequest") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a executeRequest instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        executeRequestJson.as[ExecuteRequest] should be (executeRequest)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newExecuteRequest = executeRequestJson.asOpt[ExecuteRequest]

        newExecuteRequest.get should be (executeRequest)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val executeRequestResults = executeRequestJson.validate[ExecuteRequest]

        executeRequestResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: ExecuteRequest) => valid
        ) should be (executeRequest)
      }

      it("should implicitly convert from a executeRequest instance to valid json") {
        Json.toJson(executeRequest) should be (executeRequestJson)
      }
    }
  }
}

