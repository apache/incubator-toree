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

class CommMsgSpec extends FunSpec with Matchers {
  val commMsgJson: JsValue = Json.parse("""
  {
    "comm_id": "<UUID>",
    "data": {}
  }
  """)

  val commMsgJsonWithData: JsValue = Json.parse(
    """
    {
      "comm_id": "<UUID>",
      "data": {
        "key" : {
          "foo" : "bar",
          "baz" : {
            "qux" : 3
          }
        }
      }
    }
    """.stripMargin)

  val commMsg = CommMsg(
    "<UUID>", MsgData.Empty
  )

  val commMsgWithData = CommMsg(
    "<UUID>", MsgData("key" -> Json.obj(
      "foo" -> "bar",
      "baz" -> Map("qux" -> 3)
    ))
  )

  describe("CommMsg") {
    describe("#toTypeString") {
      it("should return correct type") {
        CommMsg.toTypeString should be ("comm_msg")
      }
    }

    describe("implicit conversions") {
      it("should implicitly convert from valid json to a CommMsg instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        commMsgJson.as[CommMsg] should be (commMsg)
      }

      it("should implicitly convert json with a non-empty json data field " +
         "to a CommMsg instance") {
        commMsgJsonWithData.as[CommMsg] should be (commMsgWithData)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newCompleteRequest = commMsgJson.asOpt[CommMsg]

        newCompleteRequest.get should be (commMsg)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val CompleteRequestResults = commMsgJson.validate[CommMsg]

        CompleteRequestResults.fold(
          (invalid: Seq[(JsPath, Seq[JsonValidationError])]) => println("Failed!"),
          (valid: CommMsg) => valid
        ) should be (commMsg)
      }

      it("should implicitly convert from a CommMsg instance to valid json") {
        Json.toJson(commMsg) should be (commMsgJson)
      }

      it("should implicitly convert a CommMsg instance with non-empty json " +
         "data to valid json") {
        Json.toJson(commMsgWithData) should be (commMsgJsonWithData)
      }
    }
  }
}

