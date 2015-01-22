/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.spark.kernel.protocol.v5.stream

import java.util.UUID

import akka.actor.{ActorSelection, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.kernel.ActorLoader
import com.ibm.spark.utils.ScheduledTaskManager
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest._
import play.api.libs.json._
import com.ibm.spark.kernel.protocol.v5.content.StreamContent

import scala.concurrent.duration._

class KernelMessageStreamSpec
  extends TestKit(ActorSystem("KernelMessageStreamActorSystem"))
  with FunSpecLike with Matchers with GivenWhenThen with BeforeAndAfter
  with MockitoSugar
{

  private var mockActorLoader: ActorLoader = _
  private var mockScheduledTaskManager: MockScheduledTaskManager = _
  private var kernelMessageRelayProbe: TestProbe = _

  //
  // SHARED ELEMENTS BETWEEN TESTS
  //

  private val ExecutionCount = 3

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
    // Create a mock ActorLoader for the KernelMessageStream we are testing
    mockActorLoader = mock[ActorLoader]

    mockScheduledTaskManager = new MockScheduledTaskManager

    // Create a probe for the relay and mock the ActorLoader to return the
    // associated ActorSelection
    kernelMessageRelayProbe = TestProbe()
    val kernelMessageRelaySelection: ActorSelection =
      system.actorSelection(kernelMessageRelayProbe.ref.path.toString)
    doReturn(kernelMessageRelaySelection)
      .when(mockActorLoader).load(SystemActorType.KernelMessageRelay)
  }

  after {
    mockScheduledTaskManager.teardown()
  }

  describe("KernelMessageStream") {
    describe("#write(Int)") {
      it("should add a new byte to the internal list") {
        Given("a kernel message stream with a skeleton kernel builder")
        val kernelMessageStream = new KernelMessageStream(
          mockActorLoader, skeletonBuilder, mockScheduledTaskManager
        )

        When("a byte is written to the stream")
        val expected = 'a'
        kernelMessageStream.write(expected)

        Then("it should be appended to the internal list")
        kernelMessageStream.flush()
        val message = kernelMessageRelayProbe
          .receiveOne(1.seconds).asInstanceOf[KernelMessage]
        val executeResult = Json.parse(message.contentString).as[StreamContent]
        executeResult.text should be (expected.toString)
      }

      it("should enable periodic flushing") {
        Given("a kernel message stream with a skeleton kernel builder")
        val kernelMessageStream = new KernelMessageStream(
          mockActorLoader, skeletonBuilder, mockScheduledTaskManager
        )

        When("a byte is written to the stream")
        val expected = 'a'
        kernelMessageStream.write(expected)

        Then("it should add a task to periodically flush")
        mockScheduledTaskManager.verifyAddTaskCalled()
      }

      it("should not enable periodic flushing if already enabled") {
        Given("a kernel message stream with a skeleton kernel builder")
        val kernelMessageStream = new KernelMessageStream(
          mockActorLoader, skeletonBuilder, mockScheduledTaskManager
        )

        And("periodic flushing is already enabled")
        kernelMessageStream.write('a')
        mockScheduledTaskManager.verifyAddTaskCalled()
        mockScheduledTaskManager.resetVerify()

        When("a byte is written to the stream")
        kernelMessageStream.write('b')

        Then("it should not add a task to periodically flush")
        mockScheduledTaskManager.verifyAddTaskNotCalled()
      }
    }
    describe("#flush") {
      it("should disable periodic flushing") {
        Given("a kernel message stream with a skeleton kernel builder")
        val kernelMessageStream = new KernelMessageStream(
          mockActorLoader, skeletonBuilder, mockScheduledTaskManager
        )

        When("a byte is written to the stream")
        val expected = 'a'
        kernelMessageStream.write(expected)

        And("flush is invoked")
        kernelMessageStream.flush()

        Then("it should remove the task to periodically flush")
        mockScheduledTaskManager.verifyRemoveTaskCalled()
      }

      it("should not disable periodic flushing if not enabled") {
        Given("a kernel message stream with a skeleton kernel builder")
        val kernelMessageStream = new KernelMessageStream(
          mockActorLoader, skeletonBuilder, mockScheduledTaskManager
        )

        When("flush is invoked")
        kernelMessageStream.flush()

        Then("it should not remove the task to periodically flush")
        mockScheduledTaskManager.verifyRemoveTaskNotCalled()
      }

      it("should set the ids of the kernel message") {
        Given("a kernel message stream with a skeleton kernel builder")
        val kernelMessageStream = new KernelMessageStream(
          mockActorLoader, skeletonBuilder, mockScheduledTaskManager
        )

        When("a string is written as the result and flushed")
        val expected = "some string"
        kernelMessageStream.write(expected.getBytes)
        kernelMessageStream.flush()

        Then("the ids should be set to execute_result")
        val message = kernelMessageRelayProbe
          .receiveOne(1.seconds).asInstanceOf[KernelMessage]
        message.ids should be (Seq(MessageType.Outgoing.Stream.toString))
      }

      it("should set the message type in the header of the kernel message to an execute_result") {
        Given("a kernel message stream with a skeleton kernel builder")
        val kernelMessageStream = new KernelMessageStream(
          mockActorLoader, skeletonBuilder, mockScheduledTaskManager
        )

        When("a string is written as the result and flushed")
        val expected = "some string"
        kernelMessageStream.write(expected.getBytes)
        kernelMessageStream.flush()

        Then("the msg_type in the header should be execute_result")
        val message = kernelMessageRelayProbe
          .receiveOne(1.seconds).asInstanceOf[KernelMessage]
        message.header.msg_type should be (MessageType.Outgoing.Stream.toString)
      }

      it("should set the content string of the kernel message") {
        Given("a kernel message stream with a skeleton kernel builder")
        val kernelMessageStream = new KernelMessageStream(
          mockActorLoader, skeletonBuilder, mockScheduledTaskManager
        )

        When("a string is written as the result and flushed")
        val expected = "some string"
        kernelMessageStream.write(expected.getBytes)
        kernelMessageStream.flush()

        Then("the content string should have text/plain set to the string")
        val message = kernelMessageRelayProbe
          .receiveOne(1.seconds).asInstanceOf[KernelMessage]
        val executeResult = Json.parse(message.contentString).as[StreamContent]
        executeResult.text should be (expected)
      }
    }
  }
}
