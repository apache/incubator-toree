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

package org.apache.toree.kernel.protocol.v5

import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json.{JsPath, JsValue, Json, JsonValidationError}

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
    describe("when null") {
      it("should implicitly convert to empty json") {
        val header: Header = null
        Json.toJson(header).toString() should be ("{}")
      }
    }

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
          (invalid: Seq[(JsPath, Seq[JsonValidationError])]) => println("Failed!"),
          (valid: Header) => valid
        ) should be (header)
      }

      it("should implicitly convert from a header instance to valid json") {
        Json.toJson(header) should be (headerJson)
      }
    }
  }
}
