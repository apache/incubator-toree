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

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.JsonValidationError
import play.api.libs.json._

class HistoryRequestSpec extends AnyFunSpec with Matchers {
  val historyRequestJson: JsValue = Json.parse("""
  {
    "output": true,
    "ras": true,
    "hist_access_type": "<STRING>",
    "session": 1,
    "start": 0,
    "stop": 5,
    "n": 1,
    "pattern": "<STRING>",
    "unique": true
  }
  """)

  val historyRequest = HistoryRequest(
    true, true, "<STRING>", 1, 0, 5, 1, "<STRING>", true
  )

  describe("HistoryRequest") {
    describe("#toTypeString") {
      it("should return correct type") {
        HistoryRequest.toTypeString should be ("history_request")
      }
    }

    describe("implicit conversions") {
      it("should implicitly convert from valid json to a HistoryRequest instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        historyRequestJson.as[HistoryRequest] should be (historyRequest)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newCompleteRequest = historyRequestJson.asOpt[HistoryRequest]

        newCompleteRequest.get should be (historyRequest)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val CompleteRequestResults = historyRequestJson.validate[HistoryRequest]

        CompleteRequestResults.fold(
          (invalid: Seq[(JsPath, Seq[JsonValidationError])]) => println("Failed!"),
          (valid: HistoryRequest) => valid
        ) should be (historyRequest)
      }

      it("should implicitly convert from a HistoryRequest instance to valid json") {
        Json.toJson(historyRequest) should be (historyRequestJson)
      }
    }
  }
}

