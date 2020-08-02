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

import org.apache.toree.kernel.protocol.v5
import org.apache.toree.kernel.protocol.v5.UUID
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, Matchers, FunSpec}

import scala.collection.{immutable, mutable}

class CommStorageSpec extends FunSpec with Matchers with BeforeAndAfter
  with MockitoSugar
{
  private val TestTargetName = "some target"
  private val TestCommId = java.util.UUID.randomUUID().toString

  private var mockLinks: immutable.IndexedSeq[v5.UUID] = _
  private var mockCommCallbacks: CommCallbacks = _
  private var callbackStorage: mutable.Map[String, CommCallbacks] = _
  private var linkStorage: mutable.Map[String, immutable.IndexedSeq[UUID]] = _
  private var commStorage: CommStorage = _

  before {
    mockLinks = mock[immutable.IndexedSeq[v5.UUID]]
    mockCommCallbacks = mock[CommCallbacks]
    callbackStorage = new mutable.HashMap[String, CommCallbacks]()
    linkStorage = new mutable.HashMap[String, immutable.IndexedSeq[UUID]]()
    commStorage = new CommStorage(callbackStorage, linkStorage)
  }

  describe("CommStorage") {
    describe("#setTargetCallbacks") {
      it("should set the internal callback storage using the target") {
        commStorage.setTargetCallbacks(TestTargetName, mockCommCallbacks)

        callbackStorage(TestTargetName) should be (mockCommCallbacks)
      }

      it("should overwrite any existing callbacks for the target") {
        val otherCommCallbacks = mock[CommCallbacks]

        commStorage.setTargetCallbacks(TestTargetName, otherCommCallbacks)
        callbackStorage(TestTargetName) should be (otherCommCallbacks)

        commStorage.setTargetCallbacks(TestTargetName, mockCommCallbacks)
        callbackStorage(TestTargetName) should be (mockCommCallbacks)
      }
    }

    describe("#removeTargetCallbacks") {
      it("should remove the internal callback with the target") {
        commStorage.setTargetCallbacks(TestTargetName, mock[CommCallbacks])
        commStorage.hasTargetCallbacks(TestTargetName) should be (true)

        commStorage.removeTargetCallbacks(TestTargetName)
        commStorage.hasTargetCallbacks(TestTargetName) should be (false)
      }

      it("should return Some containing the removed callbacks") {
        val expected = mock[CommCallbacks]
        commStorage.setTargetCallbacks(TestTargetName, expected)

        val actual = commStorage.removeTargetCallbacks(TestTargetName)

        actual should be (Some(expected))
      }

      it("should return None if no callbacks removed") {
        val expected = None

        val actual = commStorage.removeTargetCallbacks(TestTargetName)

        actual should be (expected)
      }
    }

    describe("#getTargetCallbacks") {
      it("should return Some containing callbacks associated with the target") {
        val expected = mock[CommCallbacks]

        commStorage.setTargetCallbacks(TestTargetName, expected)

        val actual = commStorage.getTargetCallbacks(TestTargetName)

        actual should be (Some(expected))
      }

      it("should return None if no callbacks associated with the target") {
        val expected = None

        val actual = commStorage.getTargetCallbacks(TestTargetName)

        actual should be (expected)
      }
    }

    describe("#hasTargetCallbacks") {
      it("should return true if the target callbacks exist") {
        commStorage.setTargetCallbacks(TestTargetName, mock[CommCallbacks])

        commStorage.hasTargetCallbacks(TestTargetName) should be (true)
      }

      it("should return false if the target callbacks do not exist") {
        commStorage.hasTargetCallbacks(TestTargetName) should be (false)
      }
    }

    describe("#setTargetCommIds") {
      it("should set the internal link storage for Comm ids for the target") {
        commStorage.setTargetCommIds(TestTargetName, mockLinks)

        linkStorage(TestTargetName) should be (mockLinks)
      }

      it("should overwrite any existing internal link storage for the target") {
        val otherLinks = mock[immutable.IndexedSeq[v5.UUID]]

        commStorage.setTargetCommIds(TestTargetName, otherLinks)
        linkStorage(TestTargetName) should be (otherLinks)

        commStorage.setTargetCommIds(TestTargetName, mockLinks)
        linkStorage(TestTargetName) should be (mockLinks)
      }
    }

    describe("#removeTargetCommIds") {
      it("should remove the internal links to the target") {
        commStorage.setTargetCommIds(TestTargetName, mockLinks)
        commStorage.hasTargetCommIds(TestTargetName) should be (true)

        commStorage.removeTargetCommIds(TestTargetName)
        commStorage.hasTargetCommIds(TestTargetName) should be (false)
      }

      it("should return Some containing the removed links") {
        val expected = mock[immutable.IndexedSeq[v5.UUID]]
        commStorage.setTargetCommIds(TestTargetName, expected)

        val actual = commStorage.removeTargetCommIds(TestTargetName)

        actual should be (Some(expected))
      }

      it("should return None if no links were removed") {
        val expected = None

        val actual = commStorage.removeTargetCommIds(TestTargetName)

        actual should be (expected)
      }
    }

    describe("#removeCommIdFromTarget") {
      it("should remove the Comm id from the list linked to a target") {
        val commIds = ("1" :: TestCommId :: "2" :: Nil).toIndexedSeq

        val expected = ("1" :: "2" :: Nil).toIndexedSeq
        commStorage.setTargetCommIds(TestTargetName, commIds)

        commStorage.removeCommIdFromTarget(TestCommId)

        commStorage.getCommIdsFromTarget(TestTargetName) should be (Some(expected))
      }

      it("should return the Some containing target linked to removed Comm id") {
        val commIds = ("1" :: TestCommId :: "2" :: Nil).toIndexedSeq

        commStorage.setTargetCommIds(TestTargetName, commIds)

        commStorage.removeCommIdFromTarget(TestCommId) should be (Some(TestTargetName))
      }

      it("should return None if no Comm id was found") {
        commStorage.removeCommIdFromTarget(TestCommId) should be (None)
      }
    }

    describe("#getCommIdsFromTarget") {
      it("should return Some containing the sequence of Comm ids if found") {
        commStorage.setTargetCommIds(TestTargetName, mockLinks)
        commStorage.getCommIdsFromTarget(TestTargetName) should be (Some(mockLinks))
      }

      it("should return None if no Comm ids are found for the target") {
        commStorage.getCommIdsFromTarget(TestTargetName) should be (None)
      }
    }

    describe("#getCommIdCallbacks") {
      it("should return Some callbacks if the Comm id is linked") {
        val expected = mockCommCallbacks

        val spyCommStorage = spy(commStorage)
        doReturn(Some(TestTargetName)).when(spyCommStorage)
          .getTargetFromCommId(TestCommId)
        doReturn(Some(expected)).when(spyCommStorage)
          .getTargetCallbacks(TestTargetName)

        spyCommStorage.getCommIdCallbacks(TestCommId) should be (Some(expected))
      }

      it("should return None if the Comm id is not linked") {
        commStorage.getCommIdCallbacks(TestCommId) should be (None)
      }
    }

    describe("#getTargetFromCommId") {
      it("should return Some containing the target name if found") {
        val commIds = (TestCommId :: Nil).toIndexedSeq

        commStorage.setTargetCommIds(TestTargetName, commIds)
        commStorage.getTargetFromCommId(TestCommId) should be (Some(TestTargetName))
      }

      it("should return None if no target name is found for the Comm id") {
        commStorage.getTargetFromCommId(TestCommId) should be (None)
      }
    }

    describe("#hasTargetCommIds") {
      it("should return true if the target has Comm ids associated with it") {
        commStorage.setTargetCommIds(TestTargetName, mockLinks)
        commStorage.hasTargetCommIds(TestTargetName) should be (true)
      }

      it("should return false if the target has no Comm ids associated with it") {
        commStorage.hasTargetCommIds(TestTargetName) should be (false)
      }
    }
  }
}
