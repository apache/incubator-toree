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

package org.apache.toree.kernel.protocol.v5.content

import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json.JsonValidationError
import play.api.libs.json._

class ConnectRequestSpec extends FunSpec with Matchers {
  val connectRequestJson: JsValue = Json.parse("""
  {}
  """)

  val connectRequest: ConnectRequest = ConnectRequest()

  describe("ConnectRequest") {
    describe("#toTypeString") {
      it("should return correct type") {
        ConnectRequest.toTypeString should be ("connect_request")
      }
    }

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
          (invalid: Seq[(JsPath, Seq[JsonValidationError])]) => println("Failed!"),
          (valid: ConnectRequest) => valid
        ) should be (connectRequest)
      }

      it("should implicitly convert from a ConnectRequest instance to valid json") {
        Json.toJson(connectRequest) should be (connectRequestJson)
      }
    }
  }
}

