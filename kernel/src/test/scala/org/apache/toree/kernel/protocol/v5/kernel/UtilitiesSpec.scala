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

package org.apache.toree.kernel.protocol.v5.kernel

import akka.util.ByteString
import org.apache.toree.communication.ZMQMessage
import org.apache.toree.kernel.protocol.v5._
import org.scalatest.{FunSpec, Matchers}

/**
 * Refactored from old KernelMessageSpec.
 */
class UtilitiesSpec extends FunSpec with Matchers {
  val header: Header = Header(
    "<UUID>", "<STRING>", "<UUID>", "<STRING>", "<FLOAT>"
  )
  val parentHeader : ParentHeader = ParentHeader(
    "<PARENT-UUID>", "<PARENT-STRING>", "<PARENT-UUID>", "<PARENT-STRING>", "<PARENT-FLOAT>"
  )
  val kernelMessage = KernelMessage(
    Seq("<STRING-1>","<STRING-2>").map(x => x.getBytes),
    "<SIGNATURE>", header, parentHeader, Map(), "<STRING>"
  )

  val zmqMessage = ZMQMessage(
    ByteString("<STRING-1>".replaceAll("""\s""", "").getBytes),
    ByteString("<STRING-2>".replaceAll("""\s""", "").getBytes),
    ByteString("<IDS|MSG>".replaceAll("""\s""", "").getBytes),
    ByteString("<SIGNATURE>".replaceAll("""\s""", "").getBytes),
    ByteString(
      """
      {
          "msg_id": "<UUID>",
          "username": "<STRING>",
          "session": "<UUID>",
          "msg_type": "<STRING>",
          "version": "<FLOAT>"
      }
      """.stripMargin.replaceAll("""\s""", "").getBytes),
    ByteString(
      """
      {
          "msg_id": "<PARENT-UUID>",
          "username": "<PARENT-STRING>",
          "session": "<PARENT-UUID>",
          "msg_type": "<PARENT-STRING>",
          "version": "<PARENT-FLOAT>"
      }
      """.stripMargin.replaceAll("""\s""", "").getBytes),
    ByteString("{}".replaceAll("""\s""", "").getBytes),
    ByteString("<STRING>".replaceAll("""\s""", "").getBytes)
  )

  describe("Utilities") {
    describe("implicit #KernelMessageToZMQMessage") {
      it("should correctly convert a kernel message to a ZMQMessage") {
        Utilities.KernelMessageToZMQMessage(kernelMessage) should equal (zmqMessage)
      }
    }

    describe("implicit #ZMQMessageToKernelMessage") {
      it("should correctly convert a ZMQMessage to a kernel message") {
        Utilities.ZMQMessageToKernelMessage(zmqMessage) should equal (kernelMessage)
      }
    }

    describe("implicit conversions should be inverses of each other") {
      it("should convert back to the original message, ZMQ -> Kernel -> ZMQ") {
        Utilities.KernelMessageToZMQMessage(
          Utilities.ZMQMessageToKernelMessage(zmqMessage)
        ) should equal (zmqMessage)
      }
      it("should convert back to the original message, Kernel -> ZMQ -> Kernel") {
        Utilities.ZMQMessageToKernelMessage(
          Utilities.KernelMessageToZMQMessage(kernelMessage)
        ) should equal (kernelMessage)
      }
    }

    describe("implicit #StringToByteString") {
      it("should correctly convert a string to a ByteString") {
        val someString = "some content"
        val expected = ByteString(someString)

        Utilities.StringToByteString(someString) should be (expected)
      }
    }

    describe("implicit #ByteStringToString") {
      it("should correctly convert a ByteString to a string") {
        val expected = "some content"
        val byteString = ByteString(expected)

        Utilities.ByteStringToString(byteString) should be (expected)
      }
    }
  }
}
