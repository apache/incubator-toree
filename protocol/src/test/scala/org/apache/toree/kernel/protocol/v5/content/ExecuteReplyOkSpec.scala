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
import play.api.libs.json._

import org.apache.toree.kernel.protocol.v5._

class ExecuteReplyOkSpec extends FunSpec with Matchers {
  val executeReplyOkJson: JsValue = Json.parse("""
  {
    "status": "ok",
    "execution_count": 999,
    "payload": [],
    "user_expressions": {}
  }
  """)

  val executeReplyOk: ExecuteReplyOk = ExecuteReplyOk(
    999, Some(Payloads()), Some(UserExpressions())
  )

  describe("ExecuteReplyOk") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a executeReplyOk instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        executeReplyOkJson.as[ExecuteReplyOk] should be (executeReplyOk)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newExecuteReplyOk = executeReplyOkJson.asOpt[ExecuteReplyOk]

        newExecuteReplyOk.get should be (executeReplyOk)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val executeReplyOkResults = executeReplyOkJson.validate[ExecuteReplyOk]

        executeReplyOkResults.fold(
          (invalid: Seq[(JsPath, Seq[ValidationError])]) => println("Failed!"),
          (valid: ExecuteReplyOk) => valid
        ) should be (executeReplyOk)
      }

      it("should implicitly convert from a executeReplyOk instance to valid json") {
        Json.toJson(executeReplyOk) should be (executeReplyOkJson)
      }
    }
  }
}

