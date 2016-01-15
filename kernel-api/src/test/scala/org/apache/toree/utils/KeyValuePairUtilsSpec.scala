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

package org.apache.toree.utils

import joptsimple.util.KeyValuePair
import org.scalatest.{Matchers, FunSpec}

class KeyValuePairUtilsSpec extends FunSpec with Matchers {
  private object TestKeyValuePair {
    def apply(key: String, value: String) = KeyValuePair.valueOf(s"$key=$value")
  }

  describe("KeyValuePairUtils") {
    describe("#stringToKeyValuePairSeq") {
      it("should throw an exception when given a null string") {
        intercept[IllegalArgumentException] {
          KeyValuePairUtils.stringToKeyValuePairSeq(null)
        }
      }

      it("should convert an empty string to an empty sequence") {
        val expected = Nil
        val actual = KeyValuePairUtils.stringToKeyValuePairSeq("")

        actual should be (expected)
      }

      it("should convert a single key-value pair to a sequence with one pair") {
        val expected = Seq(TestKeyValuePair("key", "value"))
        val actual = KeyValuePairUtils.stringToKeyValuePairSeq("key=value")

        actual should be (expected)
      }

      it("should convert multiple key-value pairs using the provided delimiter") {
        val expected = Seq(
          TestKeyValuePair("key1", "value1"),
          TestKeyValuePair("key2", "value2")
        )
        val actual = KeyValuePairUtils.stringToKeyValuePairSeq(
          "key1=value1, key2=value2", ",")

        actual should be (expected)
      }

      it("should fail if the string does not contain valid key-value pairs") {
        KeyValuePairUtils.stringToKeyValuePairSeq("not valid")
      }
    }

    describe("#keyValuePairSeqToString") {
      it("should throw an exception when given a null sequence") {
        intercept[IllegalArgumentException] {
          KeyValuePairUtils.keyValuePairSeqToString(null)
        }
      }

      it("should return an empty string if the sequence is empty") {
        val expected = ""
        val actual = KeyValuePairUtils.keyValuePairSeqToString(Nil)

        actual should be (expected)
      }

      it("should generate key=value for a key-value pair") {
        val expected = "key=value"
        val actual = KeyValuePairUtils.keyValuePairSeqToString(
          Seq(TestKeyValuePair("key", "value")))

        actual should be (expected)
      }

      it("should use the provided delimiter to separate key-value pairs") {
        val expected = "key1=value1,key2=value2"
        val actual = KeyValuePairUtils.keyValuePairSeqToString(Seq(
          TestKeyValuePair("key1", "value1"),
          TestKeyValuePair("key2", "value2")
        ), ",")

        actual should be (expected)
      }

      it("should trim whitespace from keys and values") {
        val expected = "key1=value1,key2=value2"
        val actual = KeyValuePairUtils.keyValuePairSeqToString(Seq(
          TestKeyValuePair(" key1", "  value1 "),
          TestKeyValuePair("\tkey2 ", "value2\t")
        ), ",")

        actual should be (expected)
      }
    }
  }
}
