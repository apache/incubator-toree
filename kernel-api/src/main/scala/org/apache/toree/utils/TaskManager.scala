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

import java.util.concurrent.atomic.AtomicInteger
import org.slf4j.LoggerFactory
import scala.concurrent.{Future, Promise}
import java.util.concurrent._
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.apache.toree.security.KernelSecurityManager._
import TaskManager._
import scala.util.Try

/**
 * Represents a processor of tasks that has X worker threads dedicated to
 * executing the tasks.
 *
 * @param threadGroup The thread group to use with all worker threads
 * @param minimumWorkers The number of workers to spawn initially and keep
 *                       alive even when idle
 * @param maximumWorkers The max number of worker threads to spawn, defaulting
 *                   to the number of processors on the machine
 * @param keepAliveTime The maximum time in milliseconds for workers to remain
 *                      idle before shutting down
 */
class TaskManager(
  private val threadGroup: ThreadGroup = DefaultThreadGroup,
  private val maxTasks: Int = DefaultMaxTasks,
  private val minimumWorkers: Int = DefaultMinimumWorkers,
  private val maximumWorkers: Int = DefaultMaximumWorkers,
  private val keepAliveTime: Long = DefaultKeepAliveTime
) {
  protected val logger = LoggerFactory.getLogger(this.getClass.getName)

  private[utils] class ScalingThreadPoolExecutor extends ThreadPoolExecutor(
    minimumWorkers,
    maximumWorkers,
    keepAliveTime,
    TimeUnit.MILLISECONDS,
    taskQueue,
    taskManagerThreadFactory
  ) {
    protected val logger = LoggerFactory.getLogger(this.getClass.getName)

    /** Used to keep track of tasks separately from the task queue. */
    private val taskCount = new AtomicInteger(0)

    /**
     * Syncs the core pool size of the executor with the current number of
     * tasks, using the minimum worker size and maximum worker size as the
     * bounds.
     */
    private def syncPoolLimits(): Unit = {
      val totalTasks = taskCount.get()
      val newCorePoolSize =
        math.max(minimumWorkers, math.min(totalTasks, maximumWorkers))

      logger.trace(Seq(
        s"Task execution count is $totalTasks!",
        s"Updating core pool size to $newCorePoolSize!"
      ).mkString(" "))
      executor.foreach(_.setCorePoolSize(newCorePoolSize))
    }

    override def execute(r: Runnable): Unit = {
      synchronized {
        if (taskCount.incrementAndGet() > maximumWorkers)
          logger.warn(s"Exceeded $maximumWorkers workers during processing!")

        syncPoolLimits()
      }

      super.execute(r)
    }

    override def afterExecute(r: Runnable, t: Throwable): Unit = {
      super.afterExecute(r, t)

      synchronized {
        taskCount.decrementAndGet()
        syncPoolLimits()
      }
    }
  }

  private val taskManagerThreadFactory = new ThreadFactoryBuilder()
    .setThreadFactory(new ThreadFactory {
      override def newThread(r: Runnable): Thread = new Thread(threadGroup, r)
    })
    .setDaemon(true)
    .setNameFormat("task-manager-%d")
    .build

  private val taskQueue = new ArrayBlockingQueue[Runnable](maxTasks)

  @volatile
  private[utils] var executor: Option[ScalingThreadPoolExecutor] = None

  /**
   * Adds a new task to the list to execute.
   *
   * @param taskFunction The new task as a block of code
   *
   * @return Future representing the return value (or error) from the task
   */
  def add[T <: Any](taskFunction: => T): Future[T] = {
    assert(executor.nonEmpty, "Task manager not started!")

    val taskPromise = Promise[T]()

    // Construct runnable that completes the promise
    logger.trace(s"Queueing new task to be processed!")
    executor.foreach(_.execute(new Runnable {
      override def run(): Unit = {
        var threadName: String = "???"
        try {
          threadName = Try(Thread.currentThread().getName).getOrElse(threadName)
          logger.trace(s"(Thread $threadName) Executing task!")
          val result = taskFunction

          logger.trace(s"(Thread $threadName) Task finished successfully!")
          taskPromise.success(result)
        } catch {
          case ex: Throwable =>
            val exName = ex.getClass.getName
            val exMessage = Option(ex.getLocalizedMessage).getOrElse("???")
            logger.trace(
              s"(Thread $threadName) Task failed: ($exName) = $exMessage")
            taskPromise.tryFailure(ex)
        }
      }
    }))

    taskPromise.future
  }

  /**
   * Returns the count of tasks including the currently-running ones.
   *
   * @return The count of tasks
   */
  def size: Int = taskQueue.size() + executor.map(_.getActiveCount).getOrElse(0)

  /**
   * Returns whether or not there is a task in the queue to be processed.
   *
   * @return True if the internal queue is not empty, otherwise false
   */
  def hasTaskInQueue: Boolean = !taskQueue.isEmpty

  /**
   * Whether or not there is a task being executed currently.
   *
   * @return True if there is a task being executed, otherwise false
   */
  def isExecutingTask: Boolean = executor.exists(_.getActiveCount > 0)

  /**
   * Block execution (by sleeping) until all tasks currently queued up for
   * execution are processed.
   */
  def await(): Unit =
    while (!taskQueue.isEmpty || isExecutingTask) Thread.sleep(1)

  /**
   * Starts the task manager (begins processing tasks). Creates X new threads
   * in the process.
   */
  def start(): Unit = {
    logger.trace(
      s"""
         |Initializing with the following settings:
         |- $minimumWorkers core worker pool
         |- $maximumWorkers maximum workers
         |- $keepAliveTime milliseconds keep alive time
       """.stripMargin.trim)
    executor = Some(new ScalingThreadPoolExecutor)
  }

  /**
   * Restarts internal processing of tasks (removing current task).
   */
  def restart(): Unit = {
    stop()
    start()
  }

  /**
   * Stops internal processing of tasks.
   */
  def stop(): Unit = {
    executor.foreach(_.shutdownNow())
    executor = None
  }
}

/**
 * Represents constants associated with the task manager.
 */
object TaskManager {
  /** The default thread group to use with all worker threads. */
  val DefaultThreadGroup = new ThreadGroup(RestrictedGroupName)

  /** The default number of maximum tasks accepted by the task manager. */
  val DefaultMaxTasks = 200

  /**
   * The default number of workers to spawn initially and keep alive
   * even when idle.
   */
  val DefaultMinimumWorkers = 1

  /** The default maximum number of workers to spawn. */
  val DefaultMaximumWorkers = Runtime.getRuntime.availableProcessors()

  /** The default timeout in milliseconds for workers waiting for tasks. */
  val DefaultKeepAliveTime = 1000

  /**
   * The default timeout in milliseconds to wait before stopping a thread
   * if it cannot be interrupted.
   */
  val InterruptTimeout = 5000

  /** The maximum time to wait to add a task to the queue in milliseconds. */
  val MaximumTaskQueueTimeout = 10000

  /**
   * The maximum time in milliseconds to wait to queue up a thread in the
   * thread factory.
   */
  val MaximumThreadQueueTimeout = 10000
}
