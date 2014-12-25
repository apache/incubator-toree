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

// TODO: Move duplicate code to separate project (kernel and client)

import java.util.UUID

import com.ibm.spark.comm.CommCallbacks.{CloseCallback, MsgCallback, OpenCallback}
import org.mockito.AdditionalMatchers.{not => mockNot}
import org.mockito.Matchers.{eq => mockEq, _}
import org.mockito.Mockito._
import org.scalatest.OptionValues._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

class CommRegistrarSpec extends FunSpec with Matchers with MockitoSugar {
  private val TestTargetName = "some target name"
  private val TestCommId = UUID.randomUUID().toString
  private val TestOpenFunc: OpenCallback = (_, _, _, _) => {}
  private val TestMsgFunc: MsgCallback = (_, _, _) => {}
  private val TestCloseFunc: CloseCallback = (_, _, _) => {}

  private def constructMocks(targetName: String, isContained: Boolean) = {
    val mockCommCallbacks = mock[CommCallbacks]
    val mockCommStorage = mock[CommStorage]
    doReturn(isContained).when(mockCommStorage).contains(targetName)
    doReturn(mockCommCallbacks).when(mockCommStorage)(targetName)
    doReturn(mockCommCallbacks).when(mockCommCallbacks)
      .addOpenCallback(any(classOf[OpenCallback]))
    doReturn(mockCommCallbacks).when(mockCommCallbacks)
      .addMsgCallback(any(classOf[MsgCallback]))
    doReturn(mockCommCallbacks).when(mockCommCallbacks)
      .addCloseCallback(any(classOf[CloseCallback]))

    (mockCommCallbacks, mockCommStorage)
  }

  describe("CommRegistrar") {
    describe("#register") {
      it("should store a new set of callbacks with the given target name") {
        val mockCommStorage = mock[CommStorage]
        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.register(TestTargetName)

        verify(mockCommStorage)(mockEq(TestTargetName)) = any[CommCallbacks]
      }

      it("should return a new wrapper with the default target name set") {
        val mockCommStorage = mock[CommStorage]
        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.register(TestTargetName)
          .defaultTargetName.value should be (TestTargetName)
      }
    }

    describe("#link") {
      it("should create a copy of target as Comm id if target exists") {
        val mockCommCallbacks = mock[CommCallbacks]
        val mockCommStorage = mock[CommStorage]
        doReturn(true).when(mockCommStorage).contains(TestTargetName)
        doReturn(mockCommCallbacks).when(mockCommStorage)(TestTargetName)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.link(TestTargetName, TestCommId)

        verify(mockCommStorage)(TestCommId) = mockCommCallbacks
      }

      it("should create a copy of Comm id as target if Comm id exists and not target") {
        val mockCommCallbacks = mock[CommCallbacks]
        val mockCommStorage = mock[CommStorage]
        doReturn(true).when(mockCommStorage).contains(TestCommId)
        doReturn(mockCommCallbacks).when(mockCommStorage)(TestCommId)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.link(TestTargetName, TestCommId)

        verify(mockCommStorage)(TestTargetName) = mockCommCallbacks
      }

      it("should do nothing if neither target nor Comm id exist") {
        val mockCommCallbacks = mock[CommCallbacks]
        val mockCommStorage = mock[CommStorage]

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.link(TestTargetName, TestCommId)

        verify(mockCommStorage, never())(mockEq(TestTargetName)) = any[CommCallbacks]
        verify(mockCommStorage, never())(mockEq(TestCommId)) = any[CommCallbacks]
      }
    }

    describe("#has") {
      it("should return the underlying result from the storage") {
        val mockCommStorage = mock[CommStorage]
        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.has(TestTargetName)

        verify(mockCommStorage).contains(TestTargetName)
      }
    }

    describe("#retrieve") {
      it("should return the underlying result from the storage") {
        val mockCommStorage = mock[CommStorage]
        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.retrieve(TestTargetName)

        verify(mockCommStorage).get(TestTargetName)
      }
    }

    describe("#remove") {
      it("should remove the entry from the storage") {
        val mockCommStorage = mock[CommStorage]
        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.remove(TestTargetName)

        verify(mockCommStorage).remove(TestTargetName)
      }
    }

    describe("#addOpenHandler") {
      it("should add the handler if given a specific target name") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(TestTargetName, true)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.addOpenHandler(TestTargetName, TestOpenFunc)

        verify(mockCommStorage)(TestTargetName)
        verify(mockCommCallbacks).addOpenCallback(TestOpenFunc)
        verify(mockCommStorage)(TestTargetName) = mockCommCallbacks
      }

      it("should create a new set of CommCallbacks if the target is missing") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(TestTargetName, false)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.addOpenHandler(TestTargetName, TestOpenFunc)

        verify(mockCommStorage)(mockEq(TestTargetName)) =
          mockNot(mockEq(mockCommCallbacks))
      }

      it("should add the handler if not given a target name but has a default") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(TestTargetName, true)

        val commRegistrar =
          new CommRegistrar(mockCommStorage, Some(TestTargetName))

        commRegistrar.addOpenHandler(TestOpenFunc)

        verify(mockCommStorage)(TestTargetName)
        verify(mockCommCallbacks).addOpenCallback(TestOpenFunc)
        verify(mockCommStorage)(TestTargetName) = mockCommCallbacks
      }

      it("should fail if not given a target name and has no default") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(TestTargetName, true)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        intercept[IllegalArgumentException] {
          commRegistrar.addOpenHandler(TestOpenFunc)
        }
      }
    }

    describe("#addMsgHandler") {
      it("should add the handler if given a specific target name") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(TestTargetName, true)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.addMsgHandler(TestTargetName, TestMsgFunc)

        verify(mockCommStorage)(TestTargetName)
        verify(mockCommCallbacks).addMsgCallback(TestMsgFunc)
        verify(mockCommStorage)(TestTargetName) = mockCommCallbacks
      }

      it("should create a new set of CommCallbacks if the target is missing") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(TestTargetName, false)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.addMsgHandler(TestTargetName, TestMsgFunc)

        verify(mockCommStorage)(mockEq(TestTargetName)) =
          mockNot(mockEq(mockCommCallbacks))
      }

      it("should add the handler if not given a target name but has a default") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(TestTargetName, true)

        val commRegistrar =
          new CommRegistrar(mockCommStorage, Some(TestTargetName))

        commRegistrar.addMsgHandler(TestMsgFunc)

        verify(mockCommStorage)(TestTargetName)
        verify(mockCommCallbacks).addMsgCallback(TestMsgFunc)
        verify(mockCommStorage)(TestTargetName) = mockCommCallbacks
      }

      it("should fail if not given a target name and has no default") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(TestTargetName, true)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        intercept[IllegalArgumentException] {
          commRegistrar.addMsgHandler(TestMsgFunc)
        }
      }
    }

    describe("#addCloseHandler") {
      it("should add the handler if given a specific target name") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(TestTargetName, true)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.addCloseHandler(TestTargetName, TestCloseFunc)

        verify(mockCommStorage)(TestTargetName)
        verify(mockCommCallbacks).addCloseCallback(TestCloseFunc)
        verify(mockCommStorage)(TestTargetName) = mockCommCallbacks
      }

      it("should create a new set of CommCallbacks if the target is missing") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(TestTargetName, false)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.addCloseHandler(TestTargetName, TestCloseFunc)

        verify(mockCommStorage)(mockEq(TestTargetName)) =
          mockNot(mockEq(mockCommCallbacks))
      }

      it("should add the handler if not given a target name but has a default") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(TestTargetName, true)

        val commRegistrar =
          new CommRegistrar(mockCommStorage, Some(TestTargetName))

        commRegistrar.addCloseHandler(TestCloseFunc)

        verify(mockCommStorage)(TestTargetName)
        verify(mockCommCallbacks).addCloseCallback(TestCloseFunc)
        verify(mockCommStorage)(TestTargetName) = mockCommCallbacks
      }

      it("should fail if not given a target name and has no default") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(TestTargetName, true)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        intercept[IllegalArgumentException] {
          commRegistrar.addCloseHandler(TestCloseFunc)
        }
      }
    }
  }
}
