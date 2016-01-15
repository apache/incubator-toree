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

import org.scalatest.{Matchers, FunSpec}

class HeaderBuilderSpec extends FunSpec with Matchers {
  describe("HeaderBuilder") {
    describe("#create") {
      it("should set the msg_id field to a unique ID if none is provided") {
        HeaderBuilder.create("").msg_id should not be empty
      }

      it("should set the msg_id field to the argument if provided") {
        val expected = "some id"
        HeaderBuilder.create("", expected).msg_id should be (expected)
      }

      it("should set the username field to the SparkKernelInfo property") {
        val expected = SparkKernelInfo.username
        HeaderBuilder.create("").username should be (expected)
      }

      it("should set the session field to the SparkKernelInfo property") {
        val expected = SparkKernelInfo.session
        HeaderBuilder.create("").session should be (expected)
      }

      it("should set the msg_type field to the argument provided") {
        val expected = "some type"
        HeaderBuilder.create(expected).msg_type should be (expected)
      }

      it("should set the version field to the SparkKernelInfo property") {
        val expected = SparkKernelInfo.protocolVersion
        HeaderBuilder.create("").version should be (expected)
      }
    }
  }
}
