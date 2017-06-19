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

import scala.language.existentials
import java.util.concurrent._
import java.util.UUID
import com.google.common.util.concurrent.ThreadFactoryBuilder
import ScheduledTaskManager._
import scala.util.Try

/**
 * Constructs timing-based events that are periodically executed. Does not
 * support hard-killing tasks that are not interruptable.
 * @param totalThreads The total threads to create for the underlying thread
 *                     pool
 * @param defaultExecutionDelay The default delay to use before all added tasks
 * @param defaultTimeInterval The default time interval between tasks in
 *                            milliseconds
 */
class ScheduledTaskManager(
  private val totalThreads: Int = DefaultMaxThreads,
  private val defaultExecutionDelay: Long = DefaultExecutionDelay,
  private val defaultTimeInterval: Long = DefaultTimeInterval
) {
  private[utils] val _scheduler = new ScheduledThreadPoolExecutor(
    totalThreads, new ThreadFactoryBuilder()
      .setDaemon(true)
      .setNameFormat("scheduled-task-manager-%d")
      .build())
  _scheduler.setRemoveOnCancelPolicy(true)

  private val _taskMap = new ConcurrentHashMap[String, ScheduledFuture[_]]()

  /**
   * Adds the specified task to the queued list to execute at the specified
   * time interval.
   * @param executionDelay The time delay (in milliseconds) before starting
   * @param timeInterval The time interval (in milliseconds)
   * @param task The task to execute
   * @tparam T The type of return result (currently ignored)
   * @return The id of the task
   */
  def addTask[T](
    executionDelay: Long = defaultExecutionDelay,
    timeInterval: Long = defaultTimeInterval,
    task: => T
  ) = {
    val taskId = UUID.randomUUID().toString
    val runnable: Runnable = new Runnable {
      override def run(): Unit = Try(task)
    }

    // Schedule our task at the desired interval
    _taskMap.put(taskId, _scheduler.scheduleAtFixedRate(
      runnable, executionDelay, timeInterval, TimeUnit.MILLISECONDS))

    taskId
  }

  /**
   * Removes the specified task from the manager.
   * @param taskId The id of the task to remove
   * @return True if the task was removed, otherwise false
   */
  def removeTask(taskId: String): Boolean = {
    // Exit if the task with the given id does not exist
    if (taskId == null || !_taskMap.containsKey(taskId)) return false

    val future = _taskMap.remove(taskId)

    // Stop the future, but allow the current task to finish
    future.cancel(false)

    true
  }

  /**
   * Shuts down the thread pool used for task execution.
   */
  def stop() = {
    _taskMap.clear()
    _scheduler.shutdown()
  }
}

object ScheduledTaskManager {
  val DefaultMaxThreads = 4
  val DefaultExecutionDelay = 10 // 10 milliseconds
  val DefaultTimeInterval = 100 // 100 milliseconds
}
