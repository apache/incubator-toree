package com.ibm.spark.kernel.protocol.v5.content

import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class ShutdownReplySpec extends FunSpec with Matchers {
  val shutdownReplyJson: JsValue = Json.parse("""
  {
    "restart": true
  }
  """)

  val shutdownReply: ShutdownReply = ShutdownReply(
    true
  )

  describe("ShutdownReply") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a ShutdownReply instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        shutdownReplyJson.as[ShutdownReply] should be (shutdownReply)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newShutdownReply = shutdownReplyJson.asOpt[ShutdownReply]

        newShutdownReply.get should be (shutdownReply)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val ShutdownReplyResults = shutdownReplyJson.validate[ShutdownReply]

        ShutdownReplyResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: ShutdownReply) => valid
        ) should be (shutdownReply)
      }

      it("should implicitly convert from a ShutdownReply instance to valid json") {
        Json.toJson(shutdownReply) should be (shutdownReplyJson)
      }
    }
  }
}

