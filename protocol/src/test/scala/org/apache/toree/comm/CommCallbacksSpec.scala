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

// TODO: Move duplicate code to separate project (kernel and client)

import org.apache.toree.comm.CommCallbacks._
import org.apache.toree.kernel.protocol.v5._
import org.scalatest.{FunSpec, Matchers}

class CommCallbacksSpec extends FunSpec with Matchers {

  private val testOpenCallback: OpenCallback = (_, _, _, _) => {}
  private val testMsgCallback: MsgCallback = (_, _, _) => {}
  private val testCloseCallback: CloseCallback = (_, _, _) => {}

  private val failOpenCallback: OpenCallback =
    (_, _, _, _) => throw new Throwable()
  private val failMsgCallback: MsgCallback =
    (_, _, _) => throw new Throwable()
  private val failCloseCallback: CloseCallback =
    (_, _, _) => throw new Throwable()

  describe("CommCallbacks") {
    describe("#addOpenCallback") {
      it("should append the provided callback to the internal list") {
        val commCallbacks = new CommCallbacks()
          .addOpenCallback(testOpenCallback)

        commCallbacks.openCallbacks should contain (testOpenCallback)
      }
    }

    describe("#addMsgCallback") {
      it("should append the provided callback to the internal list") {
        val commCallbacks = new CommCallbacks()
          .addMsgCallback(testMsgCallback)

        commCallbacks.msgCallbacks should contain (testMsgCallback)
      }
    }

    describe("#addCloseCallback") {
      it("should append the provided callback to the internal list") {
        val commCallbacks = new CommCallbacks()
          .addCloseCallback(testCloseCallback)

        commCallbacks.closeCallbacks should contain (testCloseCallback)
      }
    }

    describe("#removeOpenCallback") {
      it("should remove the callback from the internal list") {
        val commCallbacks = new CommCallbacks()
          .addOpenCallback(testOpenCallback)
          .removeOpenCallback(testOpenCallback)

        commCallbacks.openCallbacks should not contain (testOpenCallback)
      }
    }

    describe("#removeMsgCallback") {
      it("should remove the callback from the internal list") {
        val commCallbacks = new CommCallbacks()
          .addMsgCallback(testMsgCallback)
          .removeMsgCallback(testMsgCallback)

        commCallbacks.msgCallbacks should not contain (testMsgCallback)
      }
    }

    describe("#removeCloseCallback") {
      it("should remove the callback from the internal list") {
        val commCallbacks = new CommCallbacks()
          .addCloseCallback(testCloseCallback)
          .removeCloseCallback(testCloseCallback)

        commCallbacks.closeCallbacks should not contain (testCloseCallback)
      }
    }

    describe("#executeOpenCallbacks") {
      it("should return an empty sequence of results if no callbacks exist") {
        new CommCallbacks()
          .executeOpenCallbacks(null, "", "", MsgData.Empty) shouldBe empty
      }

      it("should return a sequence of try results if callbacks exist") {
        val commCallbacks = new CommCallbacks()
          .addOpenCallback(testOpenCallback)

        val results = commCallbacks.executeOpenCallbacks(null, "", "", MsgData.Empty)

        results.head.isSuccess should be (true)
      }

      it("should return a sequence with failures if callbacks fail") {
        val commCallbacks = new CommCallbacks()
          .addOpenCallback(failOpenCallback)

        val results = commCallbacks.executeOpenCallbacks(null, "", "", MsgData.Empty)

        results.head.isFailure should be (true)
      }
    }

    describe("#executeMsgCallbacks") {
      it("should return an empty sequence of results if no callbacks exist") {
        new CommCallbacks()
          .executeMsgCallbacks(null, "", MsgData.Empty) shouldBe empty
      }

      it("should return a sequence of try results if callbacks exist") {
        val commCallbacks = new CommCallbacks()
          .addMsgCallback(testMsgCallback)

        val results = commCallbacks.executeMsgCallbacks(null, "", MsgData.Empty)

        results.head.isSuccess should be (true)
      }

      it("should return a sequence with failures if callbacks fail") {
        val commCallbacks = new CommCallbacks()
          .addMsgCallback(failMsgCallback)

        val results = commCallbacks.executeMsgCallbacks(null, "", MsgData.Empty)

        results.head.isFailure should be (true)
      }
    }

    describe("#executeCloseCallbacks") {
      it("should return an empty sequence of results if no callbacks exist") {
        new CommCallbacks()
          .executeCloseCallbacks(null, "", MsgData.Empty) shouldBe empty
      }

      it("should return a sequence of try results if callbacks exist") {
        val commCallbacks = new CommCallbacks()
          .addCloseCallback(testCloseCallback)

        val results = commCallbacks.executeCloseCallbacks(null, "", MsgData.Empty)

        results.head.isSuccess should be (true)
      }

      it("should return a sequence with failures if callbacks fail") {
        val commCallbacks = new CommCallbacks()
          .addCloseCallback(failCloseCallback)

        val results = commCallbacks.executeCloseCallbacks(null, "", MsgData.Empty)

        results.head.isFailure should be (true)
      }
    }
  }
}
