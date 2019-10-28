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

import org.apache.toree.kernel.protocol.v5.LanguageInfo
import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json.JsonValidationError
import play.api.libs.json._

class KernelInfoReplySpec extends FunSpec with Matchers {
  val kernelInfoReplyJson: JsValue = Json.parse("""
  {
    "protocol_version": "x.y.z",
    "implementation": "<name>",
    "implementation_version": "z.y.x",
    "language_info": { "name": "<some language>", "version": "a.b.c", "file_extension": "<some extension>" },
    "banner": "<some banner>"
  }
  """)

  val kernelInfoReply: KernelInfoReply = KernelInfoReply(
    "x.y.z", "<name>", "z.y.x", LanguageInfo("<some language>", "a.b.c", Some("<some extension>")), "<some banner>"
  )

  describe("KernelInfoReply") {
    describe("#toTypeString") {
      it("should return correct type") {
        KernelInfoReply.toTypeString should be ("kernel_info_reply")
      }
    }

    describe("implicit conversions") {
      it("should implicitly convert from valid json to a kernelInfoReply instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        kernelInfoReplyJson.as[KernelInfoReply] should be (kernelInfoReply)
      }

      it("should also work with asOpt") {
        // This is safer, but we lose the error information as it returns
        // None if the conversion fails
        val newKernelInfoReply = kernelInfoReplyJson.asOpt[KernelInfoReply]

        newKernelInfoReply.get should be (kernelInfoReply)
      }

      it("should also work with validate") {
        // This is the safest as it collects all error information (not just first error) and reports it
        val kernelInfoReplyResults = kernelInfoReplyJson.validate[KernelInfoReply]

        kernelInfoReplyResults.fold(
          (invalid: Seq[(JsPath, Seq[JsonValidationError])]) => println("Failed!"),
          (valid: KernelInfoReply) => valid
        ) should be (kernelInfoReply)
      }

      it("should implicitly convert from a kernelInfoReply instance to valid json") {
        Json.toJson(kernelInfoReply) should be (kernelInfoReplyJson)
      }
    }
  }
}
