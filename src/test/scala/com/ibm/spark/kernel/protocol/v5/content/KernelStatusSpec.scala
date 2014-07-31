package com.ibm.spark.kernel.protocol.v5.content

import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsValue, Json}

class KernelStatusSpec extends FunSpec with Matchers {
  val kernelStatusJson: JsValue = Json.parse("""
  {
    "execution_state": "<STRING>"
  }
  """)

  val kernelStatus: KernelStatus = KernelStatus("<STRING>")

  describe("KernelStatus") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a kernelStatus instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        kernelStatusJson.as[KernelStatus] should be (kernelStatus)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newKernelStatus = kernelStatusJson.asOpt[KernelStatus]

        newKernelStatus.get should be (kernelStatus)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val kernelStatusResults = kernelStatusJson.validate[KernelStatus]

        kernelStatusResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: KernelStatus) => valid
        ) should be (kernelStatus)
      }

      it("should implicitly convert from a kernelStatus instance to valid json") {
        Json.toJson(kernelStatus) should be (kernelStatusJson)
      }
    }
  }
}
