package com.ibm.spark.kernel.protocol.v5.content

import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class ConnectRequestSpec extends FunSpec with Matchers {
  val connectRequestJson: JsValue = Json.parse("""
  {}
  """)

  val connectRequest: ConnectRequest = ConnectRequest()

  describe("ConnectRequest") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a ConnectRequest instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        connectRequestJson.as[ConnectRequest] should be (connectRequest)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newConnectRequest = connectRequestJson.asOpt[ConnectRequest]

        newConnectRequest.get should be (connectRequest)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val ConnectRequestResults = connectRequestJson.validate[ConnectRequest]

        ConnectRequestResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: ConnectRequest) => valid
        ) should be (connectRequest)
      }

      it("should implicitly convert from a ConnectRequest instance to valid json") {
        Json.toJson(connectRequest) should be (connectRequestJson)
      }
    }
  }
}

