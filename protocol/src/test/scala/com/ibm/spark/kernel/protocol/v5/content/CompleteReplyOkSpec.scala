package com.ibm.spark.kernel.protocol.v5.content

import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class CompleteReplyOkSpec extends FunSpec with Matchers {
  val completeReplyOkJson: JsValue = Json.parse("""
  {
    "matches": [],
    "cursor_start": 1,
    "cursor_end": 5,
    "metadata": {},
    "status": "ok"
  }
  """)
  
  
  val completeReplyOk: CompleteReplyOk = CompleteReplyOk(
    List(), 1, 5, Map()
  )

  describe("CompleteReplyOk") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a CompleteReplyOk instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        completeReplyOkJson.as[CompleteReplyOk] should be (completeReplyOk)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newCompleteReplyOk = completeReplyOkJson.asOpt[CompleteReplyOk]

        newCompleteReplyOk.get should be (completeReplyOk)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val CompleteReplyOkResults = completeReplyOkJson.validate[CompleteReplyOk]

        CompleteReplyOkResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: CompleteReplyOk) => valid
        ) should be (completeReplyOk)
      }

      it("should implicitly convert from a CompleteReplyOk instance to valid json") {
        Json.toJson(completeReplyOk) should be (completeReplyOkJson)
      }
    }
  }
}

