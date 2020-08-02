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

package org.apache.toree.comm

import java.util.UUID

import org.apache.toree.kernel.protocol.v5
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar

class CommWriterSpec extends FunSpec with Matchers with BeforeAndAfter
  with MockitoSugar
{
  private val TestCommId = UUID.randomUUID().toString

  class TestCommWriter extends CommWriter(TestCommId) {
    // Stub this implementation to do nothing
    override protected[comm] def sendCommKernelMessage[
      T <: KernelMessageContent with CommContent
    ](commContent: T): Unit = {}
  }

  private var commWriter: CommWriter = _

  before {
    commWriter = spy(new TestCommWriter())
  }

  describe("CommWriter") {
    describe("#writeOpen") {
      it("should package a CommOpen instance") {
        commWriter.writeOpen("")

        verify(commWriter).sendCommKernelMessage(any[CommOpen])
      }

      it("should include the comm_id in the message") {
        val expected = TestCommId
        commWriter.writeOpen("")

        verify(commWriter).sendCommKernelMessage(
          CommOpen(comm_id = expected, target_name = "", data = MsgData.Empty))
      }

      it("should include the target name in the message") {
        val expected = "<TARGET_NAME>"
        commWriter.writeOpen(expected)

        verify(commWriter).sendCommKernelMessage(
          CommOpen(
            comm_id = TestCommId, target_name = expected, data = MsgData.Empty
          )
        )
      }

      it("should provide empty data in the message if no data is provided") {
        val expected: v5.MsgData = MsgData.Empty
        commWriter.writeOpen("")

        verify(commWriter).sendCommKernelMessage(
          CommOpen(comm_id = TestCommId, target_name = "", data = expected))
      }

      it("should include the data in the message") {
        val expected = MsgData("message" -> "a", "foo" -> "bar")
        commWriter.writeOpen("", expected)

        verify(commWriter).sendCommKernelMessage(
          CommOpen(comm_id = TestCommId, target_name = "", data = expected))
      }
    }

    describe("#writeMsg") {
      it("should send a comm_msg message to the relay") {
        commWriter.writeMsg(MsgData.Empty)

        verify(commWriter).sendCommKernelMessage(any[CommMsg])
      }

      it("should include the comm_id in the message") {
        val expected = TestCommId
        commWriter.writeMsg(MsgData.Empty)

        verify(commWriter).sendCommKernelMessage(
          CommMsg(comm_id = expected, data = MsgData.Empty))
      }

      it("should fail a require if the data is null") {
        intercept[IllegalArgumentException] {
          commWriter.writeMsg(null)
        }
      }

      it("should include the data in the message") {
        val expected = MsgData("message" -> "a")
        commWriter.writeMsg(expected)

        verify(commWriter).sendCommKernelMessage(
          CommMsg(comm_id = TestCommId, data = expected))
      }
    }

    describe("#writeClose") {
      it("should send a comm_close message to the relay") {
        commWriter.writeClose()

        verify(commWriter).sendCommKernelMessage(any[CommClose])
      }

      it("should include the comm_id in the message") {
        val expected = TestCommId
        commWriter.writeClose()

        verify(commWriter).sendCommKernelMessage(
          CommClose(comm_id = expected, data = MsgData.Empty))
      }

      it("should provide empty data in the message if no data is provided") {
        val expected: v5.MsgData = MsgData.Empty
        commWriter.writeClose()

        verify(commWriter).sendCommKernelMessage(
          CommClose(comm_id = TestCommId, data = expected))
      }

      it("should include the data in the message") {
        val expected = MsgData("message" -> "a")
        commWriter.writeClose(expected)

        verify(commWriter).sendCommKernelMessage(
          CommClose(comm_id = TestCommId, data = expected))
      }
    }

    describe("#write") {
      it("should send a comm_msg message to the relay") {
        commWriter.write(Array('a'), 0, 1)

        verify(commWriter).sendCommKernelMessage(any[CommMsg])
      }

      it("should include the comm_id in the message") {
        val expected = TestCommId
        val messageData = MsgData("message" -> "a")
        commWriter.write(Array('a'), 0, 1)

        verify(commWriter).sendCommKernelMessage(
          CommMsg(comm_id = expected, data = messageData))
      }

      it("should package the string as part of the data with a 'message' key") {
        val expected = MsgData("message" -> "a")
        commWriter.write(Array('a'), 0, 1)

        verify(commWriter).sendCommKernelMessage(
          CommMsg(comm_id = TestCommId, data = expected))
      }
    }

    describe("#flush") {
      it("should do nothing") {
        // TODO: Is this test necessary? It does nothing.
        commWriter.flush()
      }
    }

    describe("#close") {
      it("should send a comm_close message to the relay") {
        commWriter.close()

        verify(commWriter).sendCommKernelMessage(any[CommClose])
      }

      it("should include the comm_id in the message") {
        val expected = TestCommId
        commWriter.close()

        verify(commWriter).sendCommKernelMessage(
          CommClose(comm_id = expected, data = MsgData.Empty))
      }

      it("should provide empty data in the message") {
        val expected: v5.MsgData = MsgData.Empty
        commWriter.close()

        verify(commWriter).sendCommKernelMessage(
          CommClose(comm_id = TestCommId, data = expected))
      }
    }
  }
}
