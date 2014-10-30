package com.ibm.spark.kernel.protocol.v5.content

import org.scalatest.FunSuite

import org.scalatest.{Matchers, FunSpec}
import play.api.data.validation.ValidationError
import play.api.libs.json._
import com.ibm.spark.kernel.protocol.v5._

class DisplayDataSpec extends FunSpec with Matchers {
  val displayDataJson: JsValue = Json.parse("""
  {
    "source": "<STRING>",
    "data": {},
    "metadata": {}
  }
  """)

  val displayData: DisplayData = DisplayData(
    "<STRING>", Map(), Map()
  )

  describe("DisplayData") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a displayData instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        displayDataJson.as[DisplayData] should be (displayData)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newDisplayData = displayDataJson.asOpt[DisplayData]

        newDisplayData.get should be (displayData)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val displayDataResults = displayDataJson.validate[DisplayData]

        displayDataResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: DisplayData) => valid
        ) should be (displayData)
      }

      it("should implicitly convert from a displayData instance to valid json") {
        Json.toJson(displayData) should be (displayDataJson)
      }
    }
  }
}

