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

import org.apache.toree.comm.CommCallbacks.{CloseCallback, MsgCallback, OpenCallback}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

class CommRegistrarSpec extends FunSpec with Matchers with MockitoSugar
  with BeforeAndAfter
{
  private val TestTargetName = "some target name"
  private val TestCommId = UUID.randomUUID().toString
  private val TestOpenFunc: OpenCallback = (_, _, _, _) => {}
  private val TestMsgFunc: MsgCallback = (_, _, _) => {}
  private val TestCloseFunc: CloseCallback = (_, _, _) => {}

  private var commStorage: CommStorage = _
  private var commRegistrar: CommRegistrar = _

  before {
    commStorage = new CommStorage()
    commRegistrar = new CommRegistrar(commStorage)
  }

  describe("CommRegistrar") {
    describe("#withTarget") {
      it("should update the target name to the specified name") {
        val expected = TestTargetName
        val actual = commRegistrar.withTarget(expected).defaultTargetName.get

        actual should be (expected)
      }

      it("should replace the existing target name with the specified name") {
        val firstName = "some name"
        val secondName = "some other name"

        val firstRegistrar = commRegistrar.withTarget(firstName)
        val secondRegisrar = firstRegistrar.withTarget(secondName)

        firstRegistrar.defaultTargetName.get should be (firstName)
        secondRegisrar.defaultTargetName.get should be (secondName)
      }
    }

    describe("#register") {
      it("should mark the specified target name as registered") {
        commRegistrar.register(TestTargetName)

        commRegistrar.isRegistered(TestTargetName) should be (true)
      }

      it("should not replace existing callbacks if the target exists") {
        // Set up the initial collection of callbacks
        val originalRegistrar = commRegistrar.register(TestTargetName)
          .addOpenHandler(TestOpenFunc)
          .addMsgHandler(TestMsgFunc)
          .addCloseHandler(TestCloseFunc)

        // Attempt to re-register
        commRegistrar.register(TestTargetName)

        // The original callbacks should still be in the registrar
        originalRegistrar.getOpenHandlers should contain (TestOpenFunc)
        originalRegistrar.getMsgHandlers should contain (TestMsgFunc)
        originalRegistrar.getCloseHandlers should contain (TestCloseFunc)
      }

      it("should return a new wrapper with the default target name set") {
        val expected = TestTargetName

        val actual = commRegistrar.register(expected).defaultTargetName.get

        actual should be (expected)
      }
    }

    describe("#unregister") {
      it("should remove all of the associated target callbacks") {
        commRegistrar.register(TestTargetName)

        commRegistrar.isRegistered(TestTargetName) should be (true)

        commRegistrar.unregister(TestTargetName)

        commRegistrar.isRegistered(TestTargetName) should be (false)
      }

      it("should return the removed associated target callbacks") {
        // Register and add one of each handler
        commRegistrar.register(TestTargetName)
          .addOpenHandler(TestOpenFunc)
          .addMsgHandler(TestMsgFunc)
          .addCloseHandler(TestCloseFunc)

        val commCallbacks = commRegistrar.unregister(TestTargetName).get

        commCallbacks.openCallbacks should contain (TestOpenFunc)
        commCallbacks.msgCallbacks should contain (TestMsgFunc)
        commCallbacks.closeCallbacks should contain (TestCloseFunc)
      }

      it("should return None if there is no matching target registered") {
        commRegistrar.unregister(TestTargetName) should be (None)
      }
    }

    describe("#isRegistered") {
      it("should return true if the target is currently registered") {
        commRegistrar.register(TestTargetName)

        commRegistrar.isRegistered(TestTargetName) should be (true)
      }

      it("should return false if the target is not currently registered") {
        commRegistrar.register(TestTargetName)
        commRegistrar.unregister(TestTargetName)

        commRegistrar.isRegistered(TestTargetName) should be (false)
      }

      it("should return false if the target has never been registered") {
        commRegistrar.isRegistered(TestTargetName) should be (false)
      }
    }

    describe("#link") {
      it("should add the specified Comm id to the list for the target") {
        commRegistrar.link(TestTargetName, TestCommId)

        commRegistrar.getLinks(TestTargetName) should contain (TestCommId)
      }

      it("should throw an exception if no target is provided with no default") {
        intercept[AssertionError] {
          commRegistrar.link(TestCommId)
        }
      }

      it("should use the default target if it exists and no other is given") {
        commRegistrar.register(TestTargetName).link(TestCommId)

        commRegistrar.getLinks(TestTargetName) should contain (TestCommId)
      }
    }

    describe("#getTargetFromLink") {
      it("should return Some target name if found") {
        val expected = TestTargetName

        commRegistrar.register(expected).link(TestCommId)

        val actual = commRegistrar.getTargetFromLink(TestCommId).get

        actual should be (expected)
      }

      it("should return None if not found") {
        commRegistrar.getTargetFromLink(TestCommId) should be (None)
      }
    }

    describe("#getLinks") {
      it("should return a collection of links for the target") {
        commRegistrar.register(TestTargetName).link(TestCommId)

        commRegistrar.getLinks(TestTargetName) should contain (TestCommId)
      }

      it("should return an empty collection if the target does not exist") {
        commRegistrar.getLinks(TestTargetName) should be (empty)
      }

      it("should use the default target name if no name is specified") {
        val updatedCommRegistrar =
          commRegistrar.register(TestTargetName).link(TestCommId)

        updatedCommRegistrar.getLinks should contain (TestCommId)
      }

      it("should fail if not given a target name and has no default") {
        intercept[AssertionError] {
          commRegistrar.getLinks
        }
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
        commRegistrar.addOpenHandler(TestTargetName, TestOpenFunc)

        commRegistrar.getOpenHandlers(TestTargetName) should
          contain (TestOpenFunc)
      }

      it("should create a new set of CommCallbacks if the target is missing") {
        commRegistrar.addOpenHandler(TestTargetName, TestOpenFunc)

        commRegistrar.getOpenHandlers(TestTargetName) should
          contain (TestOpenFunc)
      }

      it("should add the handler if not given a target name but has a default") {
        commRegistrar.register(TestTargetName).addOpenHandler(TestOpenFunc)

        commRegistrar.getOpenHandlers(TestTargetName) should
          contain (TestOpenFunc)
      }

      it("should fail if not given a target name and has no default") {
        intercept[AssertionError] {
          commRegistrar.addOpenHandler(TestOpenFunc)
        }
      }
    }

    describe("#getOpenHandlers") {
      it("should return a collection of open handlers for the target") {
        commRegistrar.register(TestTargetName).addOpenHandler(TestOpenFunc)

        commRegistrar.getOpenHandlers(TestTargetName) should
          contain (TestOpenFunc)
      }

      it("should return an empty collection if the target does not exist") {
        commRegistrar.getOpenHandlers(TestTargetName) should be (empty)
      }

      it("should use the default target name if no name is specified") {
        val updatedCommRegistrar =
          commRegistrar.register(TestTargetName).addOpenHandler(TestOpenFunc)

        updatedCommRegistrar.getOpenHandlers should contain (TestOpenFunc)
      }

      it("should fail if not given a target name and has no default") {
        intercept[AssertionError] {
          commRegistrar.getOpenHandlers
        }
      }
    }

    describe("#removeOpenHandler") {
      it("should remove the handler if given a specific target name") {
        val updatedRegistrar =
          commRegistrar.register(TestTargetName).addOpenHandler(TestOpenFunc)
        commRegistrar.removeOpenHandler(TestTargetName, TestOpenFunc)

        commRegistrar.getOpenHandlers(TestTargetName) should
          not contain (TestOpenFunc)
      }

      it("should remove the handler if not given a target name but has a default") {
        val updatedRegistrar =
          commRegistrar.register(TestTargetName).addOpenHandler(TestOpenFunc)
        updatedRegistrar.removeOpenHandler(TestOpenFunc)

        commRegistrar.getOpenHandlers(TestTargetName) should
          not contain (TestOpenFunc)
      }

      it("should fail if not given a target name and has no default") {
        intercept[AssertionError] {
          commRegistrar.removeOpenHandler(TestOpenFunc)
        }
      }
    }

    describe("#addMsgHandler") {
      it("should add the handler if given a specific target name") {
        commRegistrar.addMsgHandler(TestTargetName, TestMsgFunc)

        commRegistrar.getMsgHandlers(TestTargetName) should
          contain (TestMsgFunc)
      }

      it("should create a new set of CommCallbacks if the target is missing") {
        commRegistrar.addMsgHandler(TestTargetName, TestMsgFunc)

        commRegistrar.getMsgHandlers(TestTargetName) should
          contain (TestMsgFunc)
      }

      it("should add the handler if not given a target name but has a default") {
        commRegistrar.register(TestTargetName).addMsgHandler(TestMsgFunc)

        commRegistrar.getMsgHandlers(TestTargetName) should
          contain (TestMsgFunc)
      }

      it("should fail if not given a target name and has no default") {
        intercept[AssertionError] {
          commRegistrar.addMsgHandler(TestMsgFunc)
        }
      }
    }
    
    describe("#getMsgHandlers") {
      it("should return a collection of msg handlers for the target") {
        commRegistrar.register(TestTargetName).addMsgHandler(TestMsgFunc)

        commRegistrar.getMsgHandlers(TestTargetName) should
          contain (TestMsgFunc)
      }

      it("should return an empty collection if the target does not exist") {
        commRegistrar.getMsgHandlers(TestTargetName) should be (empty)
      }

      it("should use the default target name if no name is specified") {
        val updatedCommRegistrar =
          commRegistrar.register(TestTargetName).addMsgHandler(TestMsgFunc)

        updatedCommRegistrar.getMsgHandlers should contain (TestMsgFunc)
      }

      it("should fail if not given a target name and has no default") {
        intercept[AssertionError] {
          commRegistrar.getMsgHandlers
        }
      }
    }

    describe("#removeMsgHandler") {
      it("should remove the handler if given a specific target name") {
        val updatedRegistrar =
          commRegistrar.register(TestTargetName).addMsgHandler(TestMsgFunc)
        commRegistrar.removeMsgHandler(TestTargetName, TestMsgFunc)

        commRegistrar.getMsgHandlers(TestTargetName) should
          not contain (TestMsgFunc)
      }

      it("should remove the handler if not given a target name but has a default") {
        val updatedRegistrar =
          commRegistrar.register(TestTargetName).addMsgHandler(TestMsgFunc)
        updatedRegistrar.removeMsgHandler(TestMsgFunc)

        commRegistrar.getMsgHandlers(TestTargetName) should
          not contain (TestMsgFunc)
      }

      it("should fail if not given a target name and has no default") {
        intercept[AssertionError] {
          commRegistrar.removeMsgHandler(TestMsgFunc)
        }
      }
    }

    describe("#addCloseHandler") {
      it("should add the handler if given a specific target name") {
        commRegistrar.addCloseHandler(TestTargetName, TestCloseFunc)

        commRegistrar.getCloseHandlers(TestTargetName) should
          contain (TestCloseFunc)
      }

      it("should create a new set of CommCallbacks if the target is missing") {
        commRegistrar.addCloseHandler(TestTargetName, TestCloseFunc)

        commRegistrar.getCloseHandlers(TestTargetName) should
          contain (TestCloseFunc)
      }

      it("should add the handler if not given a target name but has a default") {
        commRegistrar.register(TestTargetName).addCloseHandler(TestCloseFunc)

        commRegistrar.getCloseHandlers(TestTargetName) should
          contain (TestCloseFunc)
      }

      it("should fail if not given a target name and has no default") {
        intercept[AssertionError] {
          commRegistrar.addCloseHandler(TestCloseFunc)
        }
      }
    }

    describe("#getCloseHandlers") {
      it("should return a collection of Close handlers for the target") {
        commRegistrar.register(TestTargetName).addCloseHandler(TestCloseFunc)

        commRegistrar.getCloseHandlers(TestTargetName) should
          contain (TestCloseFunc)
      }

      it("should return an empty collection if the target does not exist") {
        commRegistrar.getCloseHandlers(TestTargetName) should be (empty)
      }

      it("should use the default target name if no name is specified") {
        val updatedCommRegistrar =
          commRegistrar.register(TestTargetName).addCloseHandler(TestCloseFunc)

        updatedCommRegistrar.getCloseHandlers should contain (TestCloseFunc)
      }

      it("should fail if not given a target name and has no default") {
        intercept[AssertionError] {
          commRegistrar.getCloseHandlers
        }
      }
    }
    
    describe("#removeCloseHandler") {
      it("should remove the handler if given a specific target name") {
        val updatedRegistrar =
          commRegistrar.register(TestTargetName).addCloseHandler(TestCloseFunc)
        commRegistrar.removeCloseHandler(TestTargetName, TestCloseFunc)

        commRegistrar.getCloseHandlers(TestTargetName) should
          not contain (TestCloseFunc)
      }

      it("should remove the handler if not given a target name but has a default") {
        val updatedRegistrar =
          commRegistrar.register(TestTargetName).addCloseHandler(TestCloseFunc)
        updatedRegistrar.removeCloseHandler(TestCloseFunc)

        commRegistrar.getCloseHandlers(TestTargetName) should
          not contain (TestCloseFunc)
      }

      it("should fail if not given a target name and has no default") {
        intercept[AssertionError] {
          commRegistrar.removeCloseHandler(TestCloseFunc)
        }
      }
    }
  }
}
