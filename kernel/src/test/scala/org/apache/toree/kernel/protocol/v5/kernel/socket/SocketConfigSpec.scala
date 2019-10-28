/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package org.apache.toree.kernel.protocol.v5.kernel.socket

import com.typesafe.config.ConfigFactory
import org.scalatest.{FunSpec, Matchers}
import org.slf4j.LoggerFactory
import play.api.libs.json.JsonValidationError
import play.api.libs.json.{JsPath, JsValue, Json}

class SocketConfigSpec extends FunSpec with Matchers {
  val logger = LoggerFactory.getLogger("jt4")
  //logger.error("WOOT!")

  private val jsonString: String =
    """
    {
      "stdin_port": 10000,
      "control_port": 10001,
      "hb_port": 10002,
      "shell_port": 10003,
      "iopub_port": 10004,
      "ip": "1.2.3.4",
      "transport": "tcp",
      "signature_scheme": "hmac-sha256",
      "key": ""
    }
    """.stripMargin

  val socketConfigJson: JsValue = Json.parse(jsonString)

  val socketConfigFromConfig = SocketConfig.fromConfig(ConfigFactory.parseString(jsonString))

  val socketConfig = SocketConfig(
    10000, 10001, 10002, 10003, 10004, "1.2.3.4", "tcp", "hmac-sha256", ""
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
          (invalid: Seq[(JsPath, Seq[JsonValidationError])]) => println("Failed!"),
          (valid: SocketConfig) => valid
        ) should be (socketConfig)
      }

      it("should implicitly convert from a SocketConfig instance to valid json") {
        Json.toJson(socketConfig) should be (socketConfigJson)
      }
    }
    describe("#toConfig") {
      it("should implicitly convert from valid json to a SocketConfig instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        socketConfigFromConfig should be (socketConfig)
      }
      
      it("should convert json file to SocketConfig object") {
        socketConfigFromConfig.stdin_port should be (10000)
      }
    }
  }
}

