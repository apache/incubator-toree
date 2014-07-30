package com.ibm.kernel.protocol.v5

import com.ibm.kernel.protocol.v5.socket.SocketConfig
import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsValue, Json}

class SocketConfigSpec extends FunSpec with Matchers {
  val socketConfigJson: JsValue = Json.parse("""
  {
    "stdin_port": 10000,
    "control_port": 10001,
    "hb_port": 10002,
    "shell_port": 10003,
    "iopub_port": 10004,
    "ip": "127.0.0.1",
    "transport": "tcp",
    "signature_scheme": "hmac-sha256",
    "key": ""
  }
  """)

  val socketConfig = SocketConfig(
    10000, 10001, 10002, 10003, 10004, "127.0.0.1", "tcp", "hmac-sha256", ""
  )

  describe("SocketConfig") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a SocketConfig instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        socketConfigJson.as[SocketConfig] should be (socketConfig)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newCompleteRequest = socketConfigJson.asOpt[SocketConfig]

        newCompleteRequest.get should be (socketConfig)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val CompleteRequestResults = socketConfigJson.validate[SocketConfig]

        CompleteRequestResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: SocketConfig) => valid
        ) should be (socketConfig)
      }

      it("should implicitly convert from a SocketConfig instance to valid json") {
        Json.toJson(socketConfig) should be (socketConfigJson)
      }
    }
  }
}

