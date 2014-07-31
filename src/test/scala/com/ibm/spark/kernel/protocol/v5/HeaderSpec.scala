package com.ibm.spark.kernel.protocol.v5

import org.scalatest.{Matchers, FunSpec}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class HeaderSpec extends FunSpec with Matchers {
  val headerJson: JsValue = Json.parse("""
  {
    "msg_id": "<UUID>",
    "username": "<STRING>",
    "session": "<UUID>",
    "msg_type": "<STRING>",
    "version": "<FLOAT>"
  }
  """)

  val header: Header = Header(
    "<UUID>", "<STRING>", "<UUID>", "<STRING>", "<FLOAT>"
  )

  describe("Header") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a header instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        headerJson.as[Header] should be (header)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newHeader = headerJson.asOpt[Header]

        newHeader.get should be (header)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val headerResults = headerJson.validate[Header]

        headerResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: Header) => valid
        ) should be (header)
      }

      it("should implicitly convert from a header instance to valid json") {
        Json.toJson(header) should be (headerJson)
      }
    }
  }
}
