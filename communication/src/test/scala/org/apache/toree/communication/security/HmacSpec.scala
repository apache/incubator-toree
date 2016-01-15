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

package org.apache.toree.communication.security

import java.security.NoSuchAlgorithmException

import org.scalatest.{FunSpec, Matchers}

class HmacSpec extends FunSpec with Matchers {
  describe("Hmac Object") {
    describe("#apply") {
      it("should fail if the algorithm is not available") {
        val nonEmptyKey = "FILL"
        val badAlgorithm = "One day, I want to be a real algorithm"

        intercept[NoSuchAlgorithmException] {
          val hmac = Hmac(nonEmptyKey, HmacAlgorithm(badAlgorithm))
        }
      }

      it("should succeed if the algorithm is available") {
        val goodAlgorithm = HmacAlgorithm.SHA256

        val hmac = Hmac("", goodAlgorithm)
        hmac.algorithm should be (goodAlgorithm)
      }
    }

    describe("#newMD5") {
      it("should produce an Hmac with the algorithm set to MD5") {
        val hmac = Hmac.newMD5("")

        hmac.algorithm should be(HmacAlgorithm.MD5)
      }
    }

    describe("#newSHA1") {
      it("should produce an Hmac with the algorithm set to SHA1") {
        val hmac = Hmac.newSHA1("")

        hmac.algorithm should be(HmacAlgorithm.SHA1)
      }
    }

    describe("#newSHA256") {
      it("should produce an Hmac with the algorithm set to SHA256") {
        val hmac = Hmac.newSHA256("")

        hmac.algorithm should be(HmacAlgorithm.SHA256)
      }
    }
  }

  describe("Hmac Class") {
    describe("#apply") {
      // TODO: This should really be mocked since we don't care about the
      //       results, just the send/receive to the underlying implementation
      it("should produce the correct digest") {
        val key = "12345"
        val message = "This is a test of SHA256 in action."
        val expected =
          "e60e1494b0650875fa5eb8384e357d731358c3559c1f223d69dc43ffe13570bc"
        val hmac = new Hmac(key, HmacAlgorithm.SHA256)

        hmac(message) should be(expected)
      }
    }

    describe("#digest") {
      // TODO: This should really be mocked since we don't care about the
      //       results, just the send/receive to the underlying implementation
      it("should produce the correct digest") {
        val key = "12345"
        val message = List("This is a test of SHA256 in action.")
        val expected =
          "e60e1494b0650875fa5eb8384e357d731358c3559c1f223d69dc43ffe13570bc"
        val hmac = new Hmac(key, HmacAlgorithm.SHA256)

        hmac.digest(message) should be(expected)
      }
    }
  }

  describe("HmacAlgorithm") {
    describe("#apply") {
      it("should return a value wrapping the string input") {
        val resultTypeName = HmacAlgorithm("").getClass.getName

        // NOTE: Test written this way since unable to check directly against
        //       the Scala enumeration value
        resultTypeName should be ("scala.Enumeration$Val")
      }
    }
  }
}
