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

package org.apache.toree.utils

import java.util.concurrent.{ExecutionException, RejectedExecutionException}

import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.{Eventually, ScalaFutures, TimeLimits}
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import test.utils.UncaughtExceptionSuppression

import scala.concurrent.Future
import scala.runtime.BoxedUnit

class TaskManagerSpec extends FunSpec with Matchers with MockitoSugar
  with BeforeAndAfter with ScalaFutures with UncaughtExceptionSuppression
  with Eventually with TimeLimits
{
  implicit override val patienceConfig = PatienceConfig(
    timeout = scaled(Span(30, Seconds)),
    interval = scaled(Span(5, Milliseconds))
  )
  private val MaxTestTasks = 50000
  private var taskManager: TaskManager = _

  before {
    taskManager = new TaskManager
  }

  after {
    taskManager = null
  }

  describe("TaskManager") {
    describe("#add") {
      it("should throw an exception if not started") {
        intercept[AssertionError] {
          taskManager.add {}
        }
      }

      it("should throw an exception if more tasks are added than max task size") {
        val taskManager = new TaskManager(maximumWorkers = 1, maxTasks = 1)

        taskManager.start()

        // Should fail from having too many tasks added
        intercept[RejectedExecutionException] {
          for (i <- 1 to MaxTestTasks) taskManager.add {}
        }
      }

      it("should return a Future[_] based on task provided") {
        taskManager.start()

        // Cannot check inner Future type due to type erasure
        taskManager.add { } shouldBe an [Future[_]]

        taskManager.stop()
      }

      it("should work for a task that returns nothing") {
        taskManager.start()

        val f = taskManager.add { }

        whenReady(f) { result =>
          result shouldBe a [BoxedUnit]
          taskManager.stop()
        }
      }

      it("should construct a Runnable that invokes a Promise on success") {
        taskManager.start()

        val returnValue = 3
        val f = taskManager.add { returnValue }

        whenReady(f) { result =>
          result should be (returnValue)
          taskManager.stop()
        }
      }

      it("should construct a Runnable that invokes a Promise on failure") {
        taskManager.start()

        val error = new Throwable("ERROR")
        val f = taskManager.add { throw error }

        whenReady(f.failed) { result =>
          result should be (error)
          taskManager.stop()
        }
      }

      it("should not block when adding more tasks than available threads") {
        val taskManager = new TaskManager(maximumWorkers = 1)

        taskManager.start()

        failAfter(Span(100, Milliseconds)) {
          taskManager.add { while (true) { Thread.sleep(1) } }
          taskManager.add { while (true) { Thread.sleep(1) } }
        }
      }
    }

    describe("#size") {
      it("should be zero when no tasks have been added") {
        taskManager.size should be (0)
      }

      it("should reflect queued tasks and executing tasks") {
        val taskManager = new TaskManager(maximumWorkers = 1)
        taskManager.start()

        // Fill up the task manager and then add another task to the queue
        taskManager.add { while (true) { Thread.sleep(1000) } }
        taskManager.add { while (true) { Thread.sleep(1000) } }

        taskManager.size should be (2)
      }

      it("should be one if there is only one executing task and no queued ones") {
        taskManager.start()

        taskManager.add { while (true) { Thread.sleep(1000) } }

        // Wait until task is being executed to check if the task is still in
        // the queue
        while (!taskManager.isExecutingTask) Thread.sleep(1)

        taskManager.size should be (1)

        taskManager.stop()
      }
    }

    describe("#hasTaskInQueue") {
      it("should be false when no task has been added") {
        taskManager.hasTaskInQueue should be (false)
      }

      it("should be true where there are tasks remaining in the queue") {
        val taskManager = new TaskManager(maximumWorkers = 1)
        taskManager.start()

        // Fill up the task manager and then add another task to the queue
        taskManager.add { while (true) { Thread.sleep(1000) } }
        taskManager.add { while (true) { Thread.sleep(1000) } }

        taskManager.hasTaskInQueue should be (true)
      }

      it("should be false when the only task is currently being executed") {
        taskManager.start()

        taskManager.add { while (true) { Thread.sleep(1000) } }

        // Wait until task is being executed to check if the task is still in
        // the queue
        while (!taskManager.isExecutingTask) Thread.sleep(1)

        taskManager.hasTaskInQueue should be (false)

        taskManager.stop()
      }
    }

    describe("#isExecutingTask") {
      it("should be true when a task is being executed") {
        taskManager.start()
        taskManager.add { while (true) { Thread.sleep(1000) } }

        eventually {
          taskManager.isExecutingTask should be (true)
        }

        taskManager.stop()
      }

      it("should be false when no tasks have been added") {
        taskManager.isExecutingTask should be (false)
      }

      // TODO: Timing issue on Travis CI needs to be resolved
      ignore("should be false when all tasks have finished") {
        taskManager.start()
        val f = taskManager.add { } // Really fast execution

        // Wait for up to 1 second for the task to finish
        whenReady(f, Timeout(Span(1, Seconds))) { result =>
          taskManager.isExecutingTask should be (false)
          taskManager.stop()
        }
      }
    }

    describe("#await") {
      it("should block until all tasks are completed") {
        val taskManager = new TaskManager(
          maximumWorkers = 1,
          maxTasks = MaxTestTasks
        )

        taskManager.start()

        // TODO: Need better way to ensure tasks are still running while
        // awaiting their return
        for (x <- 1 to MaxTestTasks) taskManager.add { Thread.sleep(1) }

        assume(taskManager.hasTaskInQueue)
        taskManager.await()

        taskManager.hasTaskInQueue should be (false)
        taskManager.isExecutingTask should be (false)

        taskManager.stop()
      }
    }

    describe("#start") {
      it("should create an internal thread pool executor") {
        taskManager.start()

        taskManager.executor should not be (None)

        taskManager.stop()
      }
    }

    describe("#restart") {
      it("should stop & erase the old internal thread and create a new one") {
        taskManager.start()

        val oldExecutor = taskManager.executor

        taskManager.restart()

        taskManager.executor should not be (oldExecutor)

        taskManager.stop()
      }
    }

    describe("#stop") {
      it("should attempt to interrupt the currently-running task") {
        taskManager.start()
        val f = taskManager.add { while (true) { Thread.sleep(1000) } }

        // Wait for the task to start
        while (!taskManager.isExecutingTask) Thread.sleep(1)

        // Cancel the task
        taskManager.stop()

        // Future should return an InterruptedException
        whenReady(f.failed) { result =>
          result shouldBe an [ExecutionException]
          result.getCause shouldBe an [InterruptedException]
        }
      }

      // TODO: Refactoring task manager to be parallelizable broke this ability
      //       so this will need to be reimplemented or abandoned
      ignore("should kill the thread if interrupts failed and kill enabled") {
        taskManager.start()
        val f = taskManager.add { var x = 0; while (true) { x += 1 } }

        // Wait for the task to start
        while (!taskManager.isExecutingTask) Thread.sleep(1)

        // Kill the task
        taskManager.stop()

        // Future should return ThreadDeath when killed
        whenReady(f.failed) { result =>
          result shouldBe an [ExecutionException]
          result.getCause shouldBe a [ThreadDeath]
        }
      }
    }
  }
}
