package com.ibm.spark.kernel.protocol.v5.content

import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class StreamContentSpec extends FunSpec with Matchers {
  val streamJson: JsValue = Json.parse("""
  {
    "data": "<STRING>",
    "name": "<STRING>"
  }
  """)

  val stream = StreamContent("<STRING>", "<STRING>")

  describe("HistoryRequest") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a HistoryRequest instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        streamJson.as[StreamContent] should be (stream)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newCompleteRequest = streamJson.asOpt[StreamContent]

        newCompleteRequest.get should be (stream)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val CompleteRequestResults = streamJson.validate[StreamContent]

        CompleteRequestResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: StreamContent) => valid
        ) should be (stream)
      }

      it("should implicitly convert from a HistoryRequest instance to valid json") {
        Json.toJson(stream) should be (streamJson)
      }
    }
  }
}

