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

package com.ibm.spark.kernel.protocol.v5.comm

import com.ibm.spark.kernel.protocol.v5.comm.CommCallbacks.{CloseCallback, MsgCallback, OpenCallback}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpec}
import org.scalatest.OptionValues._

import org.mockito.Mockito._
import org.mockito.Matchers.{eq => mockEq, _}
import org.mockito.AdditionalMatchers.{not => mockNot}

class CommRegistrarSpec extends FunSpec with Matchers with MockitoSugar {

  private val testTargetName = "some target name"
  private val testOpenFunc: OpenCallback = (_, _, _) => {}
  private val testMsgFunc: MsgCallback = (_, _) => {}
  private val testCloseFunc: CloseCallback = (_, _) => {}

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

        commRegistrar.register(testTargetName)

        verify(mockCommStorage)(mockEq(testTargetName)) = any[CommCallbacks]
      }

      it("should return a new wrapper with the default target name set") {
        val mockCommStorage = mock[CommStorage]
        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.register(testTargetName)
          .defaultTargetName.value should be (testTargetName)
      }
    }

    describe("#addOpenHandler") {
      it("should add the handler if given a specific target name") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(testTargetName, true)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.addOpenHandler(testTargetName, testOpenFunc)

        verify(mockCommStorage)(testTargetName)
        verify(mockCommCallbacks).addOpenCallback(testOpenFunc)
        verify(mockCommStorage)(testTargetName) = mockCommCallbacks
      }

      it("should create a new set of CommCallbacks if the target is missing") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(testTargetName, false)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.addOpenHandler(testTargetName, testOpenFunc)

        verify(mockCommStorage)(mockEq(testTargetName)) =
          mockNot(mockEq(mockCommCallbacks))
      }

      it("should add the handler if not given a target name but has a default") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(testTargetName, true)

        val commRegistrar =
          new CommRegistrar(mockCommStorage, Some(testTargetName))

        commRegistrar.addOpenHandler(testOpenFunc)

        verify(mockCommStorage)(testTargetName)
        verify(mockCommCallbacks).addOpenCallback(testOpenFunc)
        verify(mockCommStorage)(testTargetName) = mockCommCallbacks
      }

      it("should fail if not given a target name and has no default") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(testTargetName, true)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        intercept[IllegalArgumentException] {
          commRegistrar.addOpenHandler(testOpenFunc)
        }
      }
    }

    describe("#addMsgHandler") {
      it("should add the handler if given a specific target name") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(testTargetName, true)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.addMsgHandler(testTargetName, testMsgFunc)

        verify(mockCommStorage)(testTargetName)
        verify(mockCommCallbacks).addMsgCallback(testMsgFunc)
        verify(mockCommStorage)(testTargetName) = mockCommCallbacks
      }

      it("should create a new set of CommCallbacks if the target is missing") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(testTargetName, false)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.addMsgHandler(testTargetName, testMsgFunc)

        verify(mockCommStorage)(mockEq(testTargetName)) =
          mockNot(mockEq(mockCommCallbacks))
      }

      it("should add the handler if not given a target name but has a default") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(testTargetName, true)

        val commRegistrar =
          new CommRegistrar(mockCommStorage, Some(testTargetName))

        commRegistrar.addMsgHandler(testMsgFunc)

        verify(mockCommStorage)(testTargetName)
        verify(mockCommCallbacks).addMsgCallback(testMsgFunc)
        verify(mockCommStorage)(testTargetName) = mockCommCallbacks
      }

      it("should fail if not given a target name and has no default") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(testTargetName, true)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        intercept[IllegalArgumentException] {
          commRegistrar.addMsgHandler(testMsgFunc)
        }
      }
    }

    describe("#addCloseHandler") {
      it("should add the handler if given a specific target name") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(testTargetName, true)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.addCloseHandler(testTargetName, testCloseFunc)

        verify(mockCommStorage)(testTargetName)
        verify(mockCommCallbacks).addCloseCallback(testCloseFunc)
        verify(mockCommStorage)(testTargetName) = mockCommCallbacks
      }

      it("should create a new set of CommCallbacks if the target is missing") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(testTargetName, false)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.addCloseHandler(testTargetName, testCloseFunc)

        verify(mockCommStorage)(mockEq(testTargetName)) =
          mockNot(mockEq(mockCommCallbacks))
      }

      it("should add the handler if not given a target name but has a default") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(testTargetName, true)

        val commRegistrar =
          new CommRegistrar(mockCommStorage, Some(testTargetName))

        commRegistrar.addCloseHandler(testCloseFunc)

        verify(mockCommStorage)(testTargetName)
        verify(mockCommCallbacks).addCloseCallback(testCloseFunc)
        verify(mockCommStorage)(testTargetName) = mockCommCallbacks
      }

      it("should fail if not given a target name and has no default") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(testTargetName, true)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        intercept[IllegalArgumentException] {
          commRegistrar.addCloseHandler(testCloseFunc)
        }
      }
    }
  }
}
