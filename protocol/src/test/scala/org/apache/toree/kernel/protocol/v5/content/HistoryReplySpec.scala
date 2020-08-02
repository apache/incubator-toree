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

import org.apache.toree.kernel.protocol.v5._

class HistoryReplySpec extends FunSpec with Matchers {
  val historyReplyJson: JsValue = Json.parse("""
  {
    "history": ["<STRING>", "<STRING2>", "<STRING3>"]
  }
  """)

  val historyReply = HistoryReply(
    List("<STRING>", "<STRING2>", "<STRING3>")
  )

  describe("HistoryReply") {
    describe("#toTypeString") {
      it("should return correct type") {
        HistoryReply.toTypeString should be ("history_reply")
      }
    }

    describe("implicit conversions") {
      it("should implicitly convert from valid json to a HistoryReply instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        historyReplyJson.as[HistoryReply] should be (historyReply)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newCompleteRequest = historyReplyJson.asOpt[HistoryReply]

        newCompleteRequest.get should be (historyReply)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val CompleteRequestResults = historyReplyJson.validate[HistoryReply]

        CompleteRequestResults.fold(
          (invalid: Seq[(JsPath, Seq[JsonValidationError])]) => println("Failed!"),
          (valid: HistoryReply) => valid
        ) should be (historyReply)
      }

      it("should implicitly convert from a HistoryReply instance to valid json") {
        Json.toJson(historyReply) should be (historyReplyJson)
      }
    }
  }
}

