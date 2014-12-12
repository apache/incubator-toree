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

package com.ibm.spark.utils

import java.util.concurrent.ExecutionException

import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import test.utils.UncaughtExceptionSuppression

import scala.concurrent.Future
import scala.runtime.BoxedUnit

class TaskManagerSpec extends FunSpec with Matchers with MockitoSugar
  with BeforeAndAfter with ScalaFutures with UncaughtExceptionSuppression
{
  private var taskManager: TaskManager = _

  before {
    taskManager = new TaskManager
  }

  after {
    taskManager = null
  }

  describe("TaskManager") {
    describe("#add") {
      // TODO: How to verify the (Runnable, Promise[_]) stored in private queue?

      it("should return a Future[_] based on task provided") {
        // Cannot check inner Future type due to type erasure
        taskManager.add { } shouldBe an [Future[_]]
      }

      it("should work for a task that returns nothing") {
        val f = taskManager.add { }

        taskManager.start()

        whenReady(f) { result =>
          result shouldBe a [BoxedUnit]
          taskManager.stop()
        }
      }

      it("should construct a Runnable that invokes a Promise on success") {
        val returnValue = 3
        val f = taskManager.add { returnValue }

        taskManager.start()

        whenReady(f) { result =>
          result should be (returnValue)
          taskManager.stop()
        }
      }

      it("should construct a Runnable that invokes a Promise on failure") {
        val error = new Throwable("ERROR")
        val f = taskManager.add { throw error }

        taskManager.start()

        whenReady(f.failed) { result =>
          result should be (error)
          taskManager.stop()
        }
      }
    }

    describe("#tasks") {
      it("should return a sequence of Runnables not yet executed") {
        // TODO: Investigate how to validate tasks better than just a count
        for (x <- 1 to 50) taskManager.add { }

        taskManager.tasks should have size 50
      }
    }

    describe("#thread") {
      it("should return Some(Thread) if running") {
        taskManager.start()

        taskManager.thread should not be (None)

        taskManager.stop()
      }

      it("should return None if not running") {
        taskManager.thread should be (None)
      }
    }

    describe("#size") {
      it("should be zero when no tasks have been added") {
        taskManager.size should be (0)
      }

      it("should be one when a new task has been added") {
        taskManager.add {}

        taskManager.size should be (1)
      }

      it("should be zero when the only task is currently being executed") {
        taskManager.add { Thread.sleep(10000) }

        taskManager.start()

        // Wait until task is being executed to check if the task is still in
        // the queue
        while (!taskManager.isExecutingTask) Thread.sleep(1)

        taskManager.size should be (0)

        taskManager.stop()
      }
    }

    describe("#hasTaskInQueue") {
      it("should be false when no task has been added") {
        taskManager.hasTaskInQueue should be (false)
      }

      it("should be true when one task has been added but not started") {
        taskManager.add {}

        taskManager.hasTaskInQueue should be (true)
      }

      it("should be false when the only task is currently being executed") {
        taskManager.add { Thread.sleep(10000) }

        taskManager.start()

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
        taskManager.add { Thread.sleep(10000) }

        // Wait until task is being executed to check
        while (taskManager.hasTaskInQueue) Thread.sleep(1)

        taskManager.isExecutingTask should be (true)

        taskManager.stop()
      }

      it("should be false when no tasks have been added") {
        taskManager.isExecutingTask should be (false)
      }

      it("should be false when all tasks have finished") {
        taskManager.start()
        val f = taskManager.add { } // Really fast execution

        // Wait for up to 1 second for the task to finish
        whenReady(f, Timeout(Span(1, Seconds))) { result =>
          taskManager.isExecutingTask should be (false)
          taskManager.stop()
        }
      }
    }

    describe("#currentTask") {
      it("should be None when there are no tasks") {
        taskManager.currentTask should be (None)
      }

      it("should be None when there are tasks, but none are running") {
        taskManager.add { }

        taskManager.currentTask should be (None)
      }

      it("should be Some(...) when a task is being executed") {
        taskManager.add { Thread.sleep(10000) }
        taskManager.start()

        // Wait until executing task
        while (!taskManager.isExecutingTask) Thread.sleep(1)

        taskManager.currentTask should not be (None)

        taskManager.stop()
      }
    }

    describe("#isRunning") {
      it("should be false when not started") {
        taskManager.isRunning should be (false)
      }

      it("should be true after being started") {
        taskManager.start()
        taskManager.isRunning should be (true)
        taskManager.stop()
      }

      it("should be false after being stopped") {
        taskManager.start(); taskManager.stop()
        taskManager.isRunning should be (false)
      }
    }

    describe("#await") {
      it("should block until all tasks are completed") {
        // TODO: Need better way to ensure tasks are still running while
        // awaiting their return
        for (x <- 1 to 50) taskManager.add { Thread.sleep(1) }

        taskManager.start()

        assume(taskManager.hasTaskInQueue)
        taskManager.await()

        taskManager.hasTaskInQueue should be (false)
        taskManager.isExecutingTask should be (false)

        taskManager.stop()
      }
    }

    describe("#start") {
      it("should create an internal thread and start it") {
        taskManager.start()

        taskManager.thread should not be (None)

        taskManager.stop()
      }
    }

    describe("#restart") {
      it("should stop & erase the old internal thread and create a new one") {
        taskManager.start()

        val oldThread = taskManager.thread

        taskManager.restart()

        taskManager.thread should not be (oldThread)

        taskManager.stop()
      }
    }

    describe("#stop") {
      it("should attempt to interrupt the currently-running task") {
        val f = taskManager.add { Thread.sleep(10000) }
        taskManager.start()

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

      it("should kill the thread if interrupts failed and kill enabled") {
        val f = taskManager.add { var x = 0; while (true) { x += 1 } }
        taskManager.start()

        // Wait for the task to start
        while (!taskManager.isExecutingTask) Thread.sleep(1)

        // Kill the task
        taskManager.stop(true, 0)

        // Future should return ThreadDeath when killed
        whenReady(f.failed) { result =>
          result shouldBe an [ExecutionException]
          result.getCause shouldBe a [ThreadDeath]
        }
      }
    }
  }
}
