/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.toree.kernel.protocol.v5.content

import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsValue, Json}

/**
 * Created by Senkwich on 7/24/14.
 */
class ConnectReplySpec extends FunSpec with Matchers {


  val connectReplyJson: JsValue = Json.parse("""
  {
    "shell_port": 11111,
    "iopub_port": 22222,
    "stdin_port": 33333,
    "hb_port": 44444
  }
  """)

  val connectReply: ConnectReply = ConnectReply(11111,22222,33333,44444)

  describe("ConnectReply") {
    describe("#toTypeString") {
      it("should return correct type") {
        ConnectReply.toTypeString should be ("connect_reply")
      }
    }

    describe("implicit conversions") {
      it("should implicitly convert from valid json to a ConnectReply instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        connectReplyJson.as[ConnectReply] should be (connectReply)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newConnectReply = connectReplyJson.asOpt[ConnectReply]

        newConnectReply.get should be (connectReply)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val ConnectReplyResults = connectReplyJson.validate[ConnectReply]

        ConnectReplyResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: ConnectReply) => valid
        ) should be (connectReply)
      }

      it("should implicitly convert from a ConnectReply instance to valid json") {
        Json.toJson(connectReply) should be (connectReplyJson)
      }
    }
  }

}
