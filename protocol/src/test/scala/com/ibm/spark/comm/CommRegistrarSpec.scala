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

import com.ibm.spark.comm.CommCallbacks.{CloseCallback, MsgCallback, OpenCallback}
import com.ibm.spark.kernel.protocol.v5
import org.mockito.AdditionalMatchers.{not => mockNot}
import org.mockito.ArgumentMatcher
import org.mockito.Matchers.{eq => mockEq, _}
import org.mockito.Mockito._
import org.scalatest.OptionValues._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

import scala.collection.immutable.IndexedSeq

class CommRegistrarSpec extends FunSpec with Matchers with MockitoSugar {
  private val TestTargetName = "some target name"
  private val TestCommId = UUID.randomUUID().toString
  private val TestOpenFunc: OpenCallback = (_, _, _, _) => {}
  private val TestMsgFunc: MsgCallback = (_, _, _) => {}
  private val TestCloseFunc: CloseCallback = (_, _, _) => {}


  private case class ContainsThisElement[T](private val element: T)
    extends ArgumentMatcher[IndexedSeq[T]]
  {
    override def matches(list: scala.Any): Boolean =
      list.asInstanceOf[IndexedSeq[T]].contains(element)
  }

  private def constructMocks(targetName: String, hasCallbacks: Boolean) = {
    val mockCommCallbacks = mock[CommCallbacks]
    val mockCommStorage = mock[CommStorage]
    doReturn(hasCallbacks).when(mockCommStorage).hasTargetCallbacks(targetName)
    val callbacksReturn = if (hasCallbacks) Some(mockCommCallbacks) else None
    doReturn(callbacksReturn).when(mockCommStorage).getTargetCallbacks(targetName)
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

        verify(mockCommStorage)
          .setTargetCallbacks(mockEq(TestTargetName), any[CommCallbacks])
      }

      it("should not replace existing callbacks if the target exists") {
        val mockCommStorage = mock[CommStorage]
        doReturn(true).when(mockCommStorage).hasTargetCallbacks(TestTargetName)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.register(TestTargetName)

        verify(mockCommStorage, never())
          .setTargetCallbacks(anyString(), any[CommCallbacks])
      }

      it("should return a new wrapper with the default target name set") {
        val mockCommStorage = mock[CommStorage]
        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.register(TestTargetName)
          .defaultTargetName.value should be (TestTargetName)
      }
    }

    describe("#link") {
      it("should add the specified Comm id to the list for the target") {
        val mockCommStorage = mock[CommStorage]
        doReturn(None).when(mockCommStorage).getCommIdsFromTarget(anyString())

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.link(TestTargetName, TestCommId)

        verify(mockCommStorage).setTargetCommIds(
          mockEq(TestTargetName),
          argThat(ContainsThisElement(TestCommId))
        )
      }

      it("should throw an exception if no target is provided with no default") {
        val commRegistrar = new CommRegistrar(mock[CommStorage])

        intercept[IllegalArgumentException] {
          commRegistrar.link(TestCommId)
        }
      }

      it("should use the default target if it exists and no other is given") {
        val mockCommStorage = mock[CommStorage]
        doReturn(None).when(mockCommStorage).getCommIdsFromTarget(anyString())

        val commRegistrar =
          new CommRegistrar(mockCommStorage, Some(TestTargetName))

        commRegistrar.link(TestCommId)

        verify(mockCommStorage).setTargetCommIds(
          mockEq(TestTargetName),
          argThat(ContainsThisElement(TestCommId))
        )
      }
    }

    describe("#unlink") {
      it("should remove the Comm id from the underlying target") {
        val mockCommStorage = mock[CommStorage]
        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.unlink(TestCommId)

        verify(mockCommStorage).removeCommIdFromTarget(TestCommId)
      }
    }

    describe("#addOpenHandler") {
      it("should add the handler if given a specific target name") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(TestTargetName, true)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.addOpenHandler(TestTargetName, TestOpenFunc)

        verify(mockCommCallbacks).addOpenCallback(TestOpenFunc)
        verify(mockCommStorage)
          .setTargetCallbacks(TestTargetName, mockCommCallbacks)
      }

      it("should create a new set of CommCallbacks if the target is missing") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(TestTargetName, false)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.addOpenHandler(TestTargetName, TestOpenFunc)

        verify(mockCommStorage).setTargetCallbacks(
          mockEq(TestTargetName), mockNot(mockEq(mockCommCallbacks)))
      }

      it("should add the handler if not given a target name but has a default") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(TestTargetName, true)

        val commRegistrar =
          new CommRegistrar(mockCommStorage, Some(TestTargetName))

        commRegistrar.addOpenHandler(TestOpenFunc)

        verify(mockCommCallbacks).addOpenCallback(TestOpenFunc)
        verify(mockCommStorage)
          .setTargetCallbacks(TestTargetName, mockCommCallbacks)
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

        verify(mockCommCallbacks).addMsgCallback(TestMsgFunc)
        verify(mockCommStorage)
          .setTargetCallbacks(TestTargetName, mockCommCallbacks)
      }

      it("should create a new set of CommCallbacks if the target is missing") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(TestTargetName, false)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.addMsgHandler(TestTargetName, TestMsgFunc)

        verify(mockCommStorage).setTargetCallbacks(
          mockEq(TestTargetName), mockNot(mockEq(mockCommCallbacks)))
      }

      it("should add the handler if not given a target name but has a default") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(TestTargetName, true)

        val commRegistrar =
          new CommRegistrar(mockCommStorage, Some(TestTargetName))

        commRegistrar.addMsgHandler(TestMsgFunc)

        verify(mockCommCallbacks).addMsgCallback(TestMsgFunc)
        verify(mockCommStorage)
          .setTargetCallbacks(TestTargetName, mockCommCallbacks)
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

        verify(mockCommCallbacks).addCloseCallback(TestCloseFunc)
        verify(mockCommStorage)
          .setTargetCallbacks(TestTargetName, mockCommCallbacks)
      }

      it("should create a new set of CommCallbacks if the target is missing") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(TestTargetName, false)

        val commRegistrar = new CommRegistrar(mockCommStorage)

        commRegistrar.addCloseHandler(TestTargetName, TestCloseFunc)

        verify(mockCommStorage).setTargetCallbacks(
          mockEq(TestTargetName), mockNot(mockEq(mockCommCallbacks)))
      }

      it("should add the handler if not given a target name but has a default") {
        val (mockCommCallbacks, mockCommStorage) =
          constructMocks(TestTargetName, true)

        val commRegistrar =
          new CommRegistrar(mockCommStorage, Some(TestTargetName))

        commRegistrar.addCloseHandler(TestCloseFunc)

        verify(mockCommCallbacks).addCloseCallback(TestCloseFunc)
        verify(mockCommStorage)
          .setTargetCallbacks(TestTargetName, mockCommCallbacks)
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
