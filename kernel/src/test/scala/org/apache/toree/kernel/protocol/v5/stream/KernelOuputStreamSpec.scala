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

package org.apache.toree.kernel.protocol.v5.stream

import java.util.UUID

import akka.actor.{ActorSelection, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import org.apache.toree.Main
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.utils.ScheduledTaskManager
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest._
import play.api.libs.json._
import org.apache.toree.kernel.protocol.v5.content.StreamContent
import test.utils.MaxAkkaTestTimeout

class KernelOuputStreamSpec
  extends TestKit(ActorSystem("KernelOutputStreamActorSystem", None, Some(Main.getClass.getClassLoader)))
  with FunSpecLike with Matchers with GivenWhenThen with BeforeAndAfter
  with MockitoSugar
{

  private var mockActorLoader: ActorLoader = _
  private var mockScheduledTaskManager: MockScheduledTaskManager = _
  private var kernelOutputRelayProbe: TestProbe = _

  //
  // SHARED ELEMENTS BETWEEN TESTS
  //

  private val GeneratedTaskId = UUID.randomUUID().toString

  private val skeletonBuilder = KMBuilder()
    .withIds(Nil).withSignature("").withContentString("")
    .withParentHeader(Header("", "", "", "", "5.0"))

  /**
   * This stubs out the methods of the scheduled task manager and provides a
   * form of verification, which is not (easily) doable with Mockito due to the
   * call-by-name argument in addTask.
   */
  private class MockScheduledTaskManager extends ScheduledTaskManager {
    private var addTaskCalled = false
    private var removeTaskCalled = false
    private var stopCalled = false

    def verifyAddTaskCalled(): Unit = addTaskCalled should be (true)
    def verifyRemoveTaskCalled(): Unit = removeTaskCalled should be (true)
    def verifyStopCalled(): Unit = stopCalled should be (true)
    def verifyAddTaskNotCalled(): Unit = addTaskCalled should be (false)
    def verifyRemoveTaskNotCalled(): Unit = removeTaskCalled should be (false)
    def verifyStopNotCalled(): Unit = stopCalled should be (false)
    def resetVerify(): Unit = {
      addTaskCalled = false
      removeTaskCalled = false
      stopCalled = false
    }

    override def addTask[T](executionDelay: Long, timeInterval: Long, task: => T): String =
    { addTaskCalled = true; GeneratedTaskId }

    override def removeTask(taskId: String): Boolean =
    { removeTaskCalled = true; true }

    override def stop(): Unit = stopCalled = true

    def teardown(): Unit = super.stop()
  }

  before {
    // Create a mock ActorLoader for the KernelOutputStream we are testing
    mockActorLoader = mock[ActorLoader]

    mockScheduledTaskManager = new MockScheduledTaskManager

    // Create a probe for the relay and mock the ActorLoader to return the
    // associated ActorSelection
    kernelOutputRelayProbe = TestProbe()
    val kernelOutputRelaySelection: ActorSelection =
      system.actorSelection(kernelOutputRelayProbe.ref.path.toString)
    doReturn(kernelOutputRelaySelection)
      .when(mockActorLoader).load(SystemActorType.KernelMessageRelay)
  }

  after {
    mockScheduledTaskManager.teardown()
  }

  describe("KernelOutputStream") {
    describe("#write(Int)") {
      it("should add a new byte to the internal list") {
        Given("a kernel output stream with a skeleton kernel builder")
        val kernelOutputStream = new KernelOutputStream(
          mockActorLoader, skeletonBuilder, mockScheduledTaskManager
        )

        When("a byte is written to the stream")
        val expected = 'a'
        kernelOutputStream.write(expected)

        Then("it should be appended to the internal list")
        kernelOutputStream.flush()
        val message = kernelOutputRelayProbe
          .receiveOne(MaxAkkaTestTimeout).asInstanceOf[KernelMessage]
        val executeResult = Json.parse(message.contentString).as[StreamContent]
        executeResult.text should be (expected.toString)
      }

      it("should enable periodic flushing") {
        Given("a kernel output stream with a skeleton kernel builder")
        val kernelOutputStream = new KernelOutputStream(
          mockActorLoader, skeletonBuilder, mockScheduledTaskManager
        )

        When("a byte is written to the stream")
        val expected = 'a'
        kernelOutputStream.write(expected)

        Then("it should add a task to periodically flush")
        mockScheduledTaskManager.verifyAddTaskCalled()
      }

      it("should not enable periodic flushing if already enabled") {
        Given("a kernel output stream with a skeleton kernel builder")
        val kernelOutputStream = new KernelOutputStream(
          mockActorLoader, skeletonBuilder, mockScheduledTaskManager
        )

        And("periodic flushing is already enabled")
        kernelOutputStream.write('a')
        mockScheduledTaskManager.verifyAddTaskCalled()
        mockScheduledTaskManager.resetVerify()

        When("a byte is written to the stream")
        kernelOutputStream.write('b')

        Then("it should not add a task to periodically flush")
        mockScheduledTaskManager.verifyAddTaskNotCalled()
      }
    }
    describe("#flush") {
      it("should disable periodic flushing") {
        Given("a kernel output stream with a skeleton kernel builder")
        val kernelOutputStream = new KernelOutputStream(
          mockActorLoader, skeletonBuilder, mockScheduledTaskManager
        )

        When("a byte is written to the stream")
        val expected = 'a'
        kernelOutputStream.write(expected)

        And("flush is invoked")
        kernelOutputStream.flush()

        Then("it should remove the task to periodically flush")
        mockScheduledTaskManager.verifyRemoveTaskCalled()
      }

      it("should not disable periodic flushing if not enabled") {
        Given("a kernel output stream with a skeleton kernel builder")
        val kernelOutputStream = new KernelOutputStream(
          mockActorLoader, skeletonBuilder, mockScheduledTaskManager
        )

        When("flush is invoked")
        kernelOutputStream.flush()

        Then("it should not remove the task to periodically flush")
        mockScheduledTaskManager.verifyRemoveTaskNotCalled()
      }

      it("should not send empty (whitespace) messages if flag is false") {
        Given("a kernel output stream with send empty output set to false")
        val kernelOutputStream = new KernelOutputStream(
          mockActorLoader, skeletonBuilder, mockScheduledTaskManager,
          sendEmptyOutput = false
        )

        When("whitespace is created and flushed")
        val expected = "\r \r \n \t"
        kernelOutputStream.write(expected.getBytes)
        kernelOutputStream.flush()

        Then("no message should be sent")
        kernelOutputRelayProbe.expectNoMessage(MaxAkkaTestTimeout)
      }

      it("should send empty (whitespace) messages if flag is true") {
        Given("a kernel output stream with send empty output set to false")
        val kernelOutputStream = new KernelOutputStream(
          mockActorLoader, skeletonBuilder, mockScheduledTaskManager,
          sendEmptyOutput = true
        )

        When("whitespace is created and flushed")
        val expected = "\r \r \n \t"
        kernelOutputStream.write(expected.getBytes)
        kernelOutputStream.flush()

        Then("the whitespace message should have been sent")
        val message = kernelOutputRelayProbe
          .receiveOne(MaxAkkaTestTimeout).asInstanceOf[KernelMessage]
        val actual = Json.parse(message.contentString).as[StreamContent].text

        actual should be (expected)
      }

      it("should set the ids of the kernel message") {
        Given("a kernel output stream with a skeleton kernel builder")
        val kernelOutputStream = new KernelOutputStream(
          mockActorLoader, skeletonBuilder, mockScheduledTaskManager
        )

        When("a string is written as the result and flushed")
        val expected = "some string"
        kernelOutputStream.write(expected.getBytes)
        kernelOutputStream.flush()

        Then("the ids should be set to execute_result")
        val message = kernelOutputRelayProbe
          .receiveOne(MaxAkkaTestTimeout).asInstanceOf[KernelMessage]
        
        message.ids(0).deep should equal (MessageType.Outgoing.Stream.toString.getBytes.deep)
      }

      it("should set the message type in the header of the kernel message to an execute_result") {
        Given("a kernel output stream with a skeleton kernel builder")
        val kernelOutputStream = new KernelOutputStream(
          mockActorLoader, skeletonBuilder, mockScheduledTaskManager
        )

        When("a string is written as the result and flushed")
        val expected = "some string"
        kernelOutputStream.write(expected.getBytes)
        kernelOutputStream.flush()

        Then("the msg_type in the header should be execute_result")
        val message = kernelOutputRelayProbe
          .receiveOne(MaxAkkaTestTimeout).asInstanceOf[KernelMessage]
        message.header.msg_type should be (MessageType.Outgoing.Stream.toString)
      }

      it("should set the content string of the kernel message") {
        Given("a kernel output stream with a skeleton kernel builder")
        val kernelOutputStream = new KernelOutputStream(
          mockActorLoader, skeletonBuilder, mockScheduledTaskManager
        )

        When("a string is written as the result and flushed")
        val expected = "some string"
        kernelOutputStream.write(expected.getBytes)
        kernelOutputStream.flush()

        Then("the content string should have text/plain set to the string")
        val message = kernelOutputRelayProbe
          .receiveOne(MaxAkkaTestTimeout).asInstanceOf[KernelMessage]
        val executeResult = Json.parse(message.contentString).as[StreamContent]
        executeResult.text should be (expected)
      }
    }
  }
}
