package com.ibm.spark.kernel.protocol.v5.content

import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._

import com.ibm.spark.kernel.protocol.v5._

class ExecuteResultSpec extends FunSpec with Matchers {
  val executeResultJson: JsValue = Json.parse("""
  {
    "execution_count": 999,
    "data": {"text/plain": "resulty result"},
    "metadata": {}
  }
  """)

  val executeResult = ExecuteResult(
    999, Data("text/plain" -> "resulty result"), Metadata()
  )

  describe("ExecuteResult") {
    describe("#hasContent") {
      it("should be true when data has a non-empty text/plain field") {
        executeResult.hasContent should be (true)
      }

      it("should be false if data is null") {
        val executeResultNoData = ExecuteResult(
          999, null, Metadata()
        )
        executeResultNoData.hasContent should be (false)
      }

      it("should be false when data does not have a text/plain field") {
        val executeResultEmptyData = ExecuteResult(
          999, Data(), Metadata()
        )
        executeResultEmptyData.hasContent should be (false)

      }

      it("should be false if text/plain field maps to an empty string") {
        val executeResultEmptyString = ExecuteResult(
          999, Data("text/plain" -> ""), Metadata()
        )
        executeResultEmptyString.hasContent should be (false)
      }

      it("should be false if text/plain maps to null") {
        val executeResultTextPlainNull = ExecuteResult(
          999, Data("text/plain" -> null), Metadata()
        )
        executeResultTextPlainNull.hasContent should be (false)
      }
    }
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

