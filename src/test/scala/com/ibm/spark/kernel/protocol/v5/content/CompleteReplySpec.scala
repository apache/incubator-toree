package com.ibm.spark.kernel.protocol.v5.content

import org.scalatest.{Matchers, FunSpec}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsValue, Json}

/**
 * Created by Senkwich on 7/24/14.
 */
class CompleteReplySpec extends FunSpec with Matchers {


  val completeReplyJson: JsValue = Json.parse("""
  {
    "matches": [],
    "cursor_start": 1,
    "cursor_end": 5,
    "metadata": {},
    "status": "<STRING>"
  }
  """)

  val completeReply: CompleteReply = CompleteReply(
    List(), 1, 5, Map(), "<STRING>", None, None, None
  )

  describe("CompleteReply") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a CompleteReply instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        completeReplyJson.as[CompleteReply] should be (completeReply)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newCompleteReply = completeReplyJson.asOpt[CompleteReply]

        newCompleteReply.get should be (completeReply)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val CompleteReplyResults = completeReplyJson.validate[CompleteReply]

        CompleteReplyResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: CompleteReply) => valid
        ) should be (completeReply)
      }

      it("should implicitly convert from a CompleteReply instance to valid json") {
        Json.toJson(completeReply) should be (completeReplyJson)
      }
    }
  }

}
