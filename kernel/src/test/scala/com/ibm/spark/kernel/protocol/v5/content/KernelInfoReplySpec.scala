package com.ibm.spark.kernel.protocol.v5.content

import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class KernelInfoReplySpec extends FunSpec with Matchers {
  val kernelInfoReplyJson: JsValue = Json.parse("""
  {
    "protocol_version": "x.y.z",
    "implementation": "<name>",
    "implementation_version": "z.y.x",
    "language": "<some language>",
    "language_version": "a.b.c",
    "banner": "<some banner>"
  }
  """)

  val kernelInfoReply: KernelInfoReply = KernelInfoReply(
    "x.y.z", "<name>", "z.y.x", "<some language>", "a.b.c", "<some banner>"
  )

  describe("KernelInfoReply") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a kernelInfoReply instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        kernelInfoReplyJson.as[KernelInfoReply] should be (kernelInfoReply)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newKernelInfoReply = kernelInfoReplyJson.asOpt[KernelInfoReply]

        newKernelInfoReply.get should be (kernelInfoReply)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val kernelInfoReplyResults = kernelInfoReplyJson.validate[KernelInfoReply]

        kernelInfoReplyResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: KernelInfoReply) => valid
        ) should be (kernelInfoReply)
      }

      it("should implicitly convert from a kernelInfoReply instance to valid json") {
        Json.toJson(kernelInfoReply) should be (kernelInfoReplyJson)
      }
    }
  }
}

