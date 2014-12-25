/*
 * Copyright 2014 IBM Corp.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.ibm.spark.comm

import java.util.UUID

import com.ibm.spark.kernel.protocol.v5
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content._
import org.mockito.Matchers.{eq => mockEq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, BeforeAndAfter, Matchers}


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
          CommOpen(comm_id = expected, target_name = "", data = Data()))
      }

      it("should include the target name in the message") {
        val expected = "<TARGET_NAME>"
        commWriter.writeOpen(expected)

        verify(commWriter).sendCommKernelMessage(
          CommOpen(comm_id = TestCommId, target_name = expected, data = Data()))
      }

      it("should provide empty data in the message if no data is provided") {
        val expected: v5.Data = Data()
        commWriter.writeOpen("")

        verify(commWriter).sendCommKernelMessage(
          CommOpen(comm_id = TestCommId, target_name = "", data = expected))
      }

      it("should include the data in the message") {
        val expected = Data("some key" -> "some value")
        commWriter.writeOpen("", expected)

        verify(commWriter).sendCommKernelMessage(
          CommOpen(comm_id = TestCommId, target_name = "", data = expected))
      }
    }

    describe("#writeMsg") {
      it("should send a comm_msg message to the relay") {
        commWriter.writeMsg(Data())

        verify(commWriter).sendCommKernelMessage(any[CommMsg])
      }

      it("should include the comm_id in the message") {
        val expected = TestCommId
        commWriter.writeMsg(Data())

        verify(commWriter).sendCommKernelMessage(
          CommMsg(comm_id = expected, data = Data()))
      }

      it("should fail a require if the data is null") {
        intercept[IllegalArgumentException] {
          commWriter.writeMsg(null)
        }
      }

      it("should include the data in the message") {
        val expected = Data("some key" -> "some value")
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
          CommClose(comm_id = expected, data = Data()))
      }

      it("should provide empty data in the message if no data is provided") {
        val expected: v5.Data = Data()
        commWriter.writeClose()

        verify(commWriter).sendCommKernelMessage(
          CommClose(comm_id = TestCommId, data = expected))
      }

      it("should include the data in the message") {
        val expected = Data("some key" -> "some value")
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
        val messageData = Data("message" -> "a")
        commWriter.write(Array('a'), 0, 1)

        verify(commWriter).sendCommKernelMessage(
          CommMsg(comm_id = expected, data = messageData))
      }

      it("should package the string as part of the data with a 'message' key") {
        val expected = Data("message" -> "a")
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
          CommClose(comm_id = expected, data = Data()))
      }

      it("should provide empty data in the message") {
        val expected: v5.Data = Data()
        commWriter.close()

        verify(commWriter).sendCommKernelMessage(
          CommClose(comm_id = TestCommId, data = expected))
      }
    }
  }
}
