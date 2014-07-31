package com.ibm.kernel.protocol.v5.content


import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class ExecuteResultSpec extends FunSpec with Matchers {
  val executeResultJson: JsValue = Json.parse("""
  {
    "execution_count": 999,
    "data": {},
    "metadata": {}
  }
  """)

  val executeResult = ExecuteResult(
    999, Map(), Map()
  )

  describe("ExecuteResult") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a ExecuteResult instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        executeResultJson.as[ExecuteResult] should be (executeResult)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newCompleteRequest = executeResultJson.asOpt[ExecuteResult]

        newCompleteRequest.get should be (executeResult)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val CompleteRequestResults = executeResultJson.validate[ExecuteResult]

        CompleteRequestResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: ExecuteResult) => valid
        ) should be (executeResult)
      }

      it("should implicitly convert from a ExecuteResult instance to valid json") {
        Json.toJson(executeResult) should be (executeResultJson)
      }
    }
  }
}

