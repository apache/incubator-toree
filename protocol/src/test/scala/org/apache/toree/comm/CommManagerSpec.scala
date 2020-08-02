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

import org.apache.toree.comm.CommCallbacks.{CloseCallback, OpenCallback}
import org.apache.toree.kernel.protocol.v5
import org.apache.toree.kernel.protocol.v5.UUID
import org.apache.toree.kernel.protocol.v5.content.{CommClose, CommOpen}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, Matchers, FunSpec}
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => mockEq, _}

class CommManagerSpec extends FunSpec with Matchers with BeforeAndAfter
  with MockitoSugar
{
  private val TestTargetName = "some target"
  private val TestCommId = java.util.UUID.randomUUID().toString

  /** Creates a new Comm Manager, filling in the Comm writer method. */
  private def newCommManager(
    commRegistrar: CommRegistrar,
    commWriter: CommWriter
  ): CommManager = new CommManager(commRegistrar) {
    override protected def newCommWriter(commId: UUID): CommWriter = commWriter
  }

  private var mockCommWriter: CommWriter = _
  private var mockCommRegistrar: CommRegistrar = _
  private var commManager: CommManager = _

  before {
    mockCommWriter = mock[CommWriter]
    mockCommRegistrar = mock[CommRegistrar]
    doReturn(mockCommRegistrar).when(mockCommRegistrar)
      .register(anyString())
    doReturn(mockCommRegistrar).when(mockCommRegistrar)
      .addOpenHandler(any(classOf[OpenCallback]))
    doReturn(mockCommRegistrar).when(mockCommRegistrar)
      .addCloseHandler(any(classOf[CloseCallback]))
    doReturn(mockCommRegistrar).when(mockCommRegistrar)
      .withTarget(anyString())

    commManager = newCommManager(mockCommRegistrar, mockCommWriter)
  }

  describe("CommManager") {
    describe("#withTarget") {
      it("should return a registrar using the target name provided") {
        val commRegistrar = commManager.withTarget(TestTargetName)

        verify(commRegistrar).withTarget(TestTargetName)
      }
    }

    describe("#register") {
      it("should register the target name provided") {
        commManager.register(TestTargetName)

        verify(mockCommRegistrar).register(TestTargetName)
      }

      // TODO: Is there a better/cleaner way to assert the contents of the callback?
      it("should add a link callback to the received open events") {
        var linkFunc: OpenCallback = null

        // Setup used to extract the function of the callback
        doAnswer(new Answer[CommRegistrar]() {
          override def answer(p1: InvocationOnMock): CommRegistrar = {
            linkFunc = p1.getArguments.head.asInstanceOf[OpenCallback]
            mockCommRegistrar
          }
        }).when(mockCommRegistrar).addOpenHandler(any(classOf[OpenCallback]))

        // Call register and verify that the underlying registrar method called
        commManager.register(TestTargetName)
        verify(mockCommRegistrar).addOpenHandler(any(classOf[OpenCallback]))

        // Trigger the callback to test what it does
        linkFunc(mock[CommWriter], TestCommId, TestTargetName, v5.MsgData.Empty)
        verify(mockCommRegistrar).link(TestTargetName, TestCommId)
      }

      // TODO: Is there a better/cleaner way to assert the contents of the callback?
      it("should add an unlink callback to the received close events") {
        var unlinkFunc: CloseCallback = null

        // Setup used to extract the function of the callback
        doAnswer(new Answer[CommRegistrar]() {
          override def answer(p1: InvocationOnMock): CommRegistrar = {
            unlinkFunc = p1.getArguments.head.asInstanceOf[CloseCallback]
            mockCommRegistrar
          }
        }).when(mockCommRegistrar).addCloseHandler(any(classOf[CloseCallback]))

        // Call register and verify that the underlying registrar method called
        commManager.register(TestTargetName)
        verify(mockCommRegistrar).addCloseHandler(any(classOf[CloseCallback]))

        // Trigger the callback to test what it does
        unlinkFunc(mock[CommWriter], TestCommId, v5.MsgData.Empty)
        verify(mockCommRegistrar).unlink(TestCommId)
      }
    }

    describe("#unregister") {
      it("should remove the target from the collection of targets") {
        val commManager = newCommManager(
          new CommRegistrar(new CommStorage()),
          mockCommWriter
        )

        commManager.register(TestTargetName)
        commManager.unregister(TestTargetName)

        commManager.isRegistered(TestTargetName) should be (false)
      }
    }

    describe("#isRegistered") {
      it("should return true if the target is currently registered") {
        val commManager = newCommManager(
          new CommRegistrar(new CommStorage()),
          mockCommWriter
        )

        commManager.register(TestTargetName)

        commManager.isRegistered(TestTargetName) should be (true)
      }

      it("should return false if the target is not currently registered") {
        val commManager = newCommManager(
          new CommRegistrar(new CommStorage()),
          mockCommWriter
        )

        commManager.register(TestTargetName)
        commManager.unregister(TestTargetName)

        commManager.isRegistered(TestTargetName) should be (false)
      }

      it("should return false if the target has never been registered") {
        val commManager = newCommManager(
          new CommRegistrar(new CommStorage()),
          mockCommWriter
        )

        commManager.isRegistered(TestTargetName) should be (false)
      }
    }

    describe("#open") {
      it("should return a new CommWriter instance that links during open") {
        val commWriter = commManager.open(TestTargetName, v5.MsgData.Empty)

        commWriter.writeOpen(TestTargetName)

        // Should have been executed once during commManager.open(...) and
        // another time with the call above
        verify(mockCommRegistrar, times(2))
          .link(mockEq(TestTargetName), any[v5.UUID])
      }

      it("should return a new CommWriter instance that unlinks during close") {
        val commWriter = commManager.open(TestTargetName, v5.MsgData.Empty)

        commWriter.writeClose(v5.MsgData.Empty)

        verify(mockCommRegistrar).unlink(any[v5.UUID])
      }

      it("should initiate a comm_open") {
        commManager.open(TestTargetName, v5.MsgData.Empty)

        verify(mockCommWriter).writeOpen(TestTargetName, v5.MsgData.Empty)
      }
    }
  }

}
