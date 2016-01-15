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

import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{TimeUnit, CountDownLatch}

import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Milliseconds, Span}
import org.scalatest.{BeforeAndAfter, Matchers, FunSpec}

class ScheduledTaskManagerSpec extends FunSpec with Matchers with BeforeAndAfter
  with Eventually
{
  private val TestTimeInterval = 30
  private val MaximumChecks = 3
  private val TimeoutScale = 3
  private var scheduledTaskManager: ScheduledTaskManager = _
  private var scheduleVerifier: ScheduleVerifier = _

  implicit override val patienceConfig = PatienceConfig(
    timeout = scaled(Span(
      TestTimeInterval * MaximumChecks * TimeoutScale, Milliseconds)),
    interval = scaled(Span(TestTimeInterval / 2, Milliseconds))
  )

  private class ScheduleVerifier {
    @volatile private var checkinTimes: List[Long] = Nil
    private val taskRun = new AtomicBoolean(false)

    def task() = {
      if (checkinTimes.length < MaximumChecks)
        checkinTimes = checkinTimes :+ Calendar.getInstance().getTimeInMillis
      taskRun.set(true)
    }

    def shouldNotRunAnymore(milliseconds: Long) = eventually {
      val offset: Int = (milliseconds * 0.5).toInt

      // Clean the state and wait to see if the task is executed again
      taskRun.set(false)
      Thread.sleep(milliseconds + offset)

      taskRun.get() should be (false)
    }

    def shouldRunEvery(milliseconds: Long) = {
      // 50% +/-
      val offset: Int = (milliseconds * 0.5).toInt

      eventually {
        // Assert we have the desired number of checks
        checkinTimes.length should be (MaximumChecks)

        checkinTimes.take(checkinTimes.length - 1).zip(
          checkinTimes.takeRight(checkinTimes.length - 1)
        ).foreach({ times =>
          val firstTime = times._1
          val secondTime = times._2
          (secondTime - firstTime) should (
            be >= milliseconds - offset and
              be <= milliseconds + offset)
        })
      }
    }
  }

  before {
    scheduledTaskManager = new ScheduledTaskManager
    scheduleVerifier = new ScheduleVerifier
  }

  after {
    scheduledTaskManager.stop()
  }

  describe("ScheduledTaskManager") {
    describe("#addTask") {
      // TODO: This is failing frequently due to some sort of timing problem
      ignore("should add a new task to be executed periodically") {
        scheduledTaskManager.addTask(timeInterval = TestTimeInterval,
          task = scheduleVerifier.task())

        scheduleVerifier.shouldRunEvery(TestTimeInterval)
      }
    }

    describe("#removeTask") {
      it("should stop and remove the task if it exists") {
        val taskId = scheduledTaskManager.addTask(
          timeInterval = TestTimeInterval,
          task = scheduleVerifier.task()
        )

        scheduledTaskManager.removeTask(taskId)

        scheduleVerifier.shouldNotRunAnymore(TestTimeInterval)
      }

      it("should return true if the task was removed") {
        val taskId = scheduledTaskManager.addTask(
          timeInterval = TestTimeInterval,
          task = scheduleVerifier.task()
        )

        scheduledTaskManager.removeTask(taskId) should be (true)
      }

      it("should return false if the task does not exist") {
        scheduledTaskManager.removeTask("") should be (false)
      }
    }
  }
}
