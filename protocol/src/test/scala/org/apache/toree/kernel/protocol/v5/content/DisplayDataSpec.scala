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

import org.scalatest.FunSuite

import org.scalatest.{Matchers, FunSpec}
import play.api.libs.json.JsonValidationError
import play.api.libs.json._
import org.apache.toree.kernel.protocol.v5._

class DisplayDataSpec extends FunSpec with Matchers {
  val displayDataJson: JsValue = Json.parse("""
  {
    "source": "<STRING>",
    "data": {},
    "metadata": {}
  }
  """)

  val displayData: DisplayData = DisplayData(
    "<STRING>", Map(), Map()
  )

  describe("DisplayData") {
    describe("#toTypeString") {
      it("should return correct type") {
        DisplayData.toTypeString should be ("display_data")
      }
    }

    describe("implicit conversions") {
      it("should implicitly convert from valid json to a displayData instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        displayDataJson.as[DisplayData] should be (displayData)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newDisplayData = displayDataJson.asOpt[DisplayData]

        newDisplayData.get should be (displayData)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val displayDataResults = displayDataJson.validate[DisplayData]

        displayDataResults.fold(
          (invalid: Seq[(JsPath, Seq[JsonValidationError])]) => println("Failed!"),
          (valid: DisplayData) => valid
        ) should be (displayData)
      }

      it("should implicitly convert from a displayData instance to valid json") {
        Json.toJson(displayData) should be (displayDataJson)
      }
    }
  }
}

