package com.ibm.spark.utils

import scala.concurrent.{promise, Future}
import java.util.concurrent._

import com.ibm.spark.security.KernelSecurityManager._
import TaskManager._

/**
 * Represents a processor of tasks that has X worker threads dedicated to
 * executing the tasks.
 *
 * @param threadGroup The thread group to use with all worker threads
 * @param initialWorkers The number of workers to spawn initially
 * @param maxWorkers The max number of worker threads to spawn, defaulting
 *                   to the number of processors on the machine
 * @param keepAliveTime The maximum time in milliseconds for workers to remain
 *                      idle before shutting down
 */
class TaskManager(
  private val threadGroup: ThreadGroup = DefaultThreadGroup,
  private val maxTasks: Int = DefaultMaxTasks,
  private val initialWorkers: Int = DefaultInitialWorkers,
  private val maxWorkers: Int = DefaultMaxWorkers,
  private val keepAliveTime: Long = DefaultKeepAliveTime
) {
  private class TaskManagerThreadFactory extends ThreadFactory {
    override def newThread(r: Runnable): Thread = {
      new Thread(threadGroup, r)
    }
  }

  private val taskManagerThreadFactory = new TaskManagerThreadFactory
  private val taskQueue = new ArrayBlockingQueue[Runnable](maxTasks)

  @volatile private[utils] var executor: Option[ThreadPoolExecutor] = None

  /**
   * Adds a new task to the list to execute.
   *
   * @param taskFunction The new task as a block of code
   *
   * @return Future representing the return value (or error) from the task
   */
  def add[T <: Any](taskFunction: => T): Future[T] = {
    assert(executor.nonEmpty, "Task manager not started!")

    val taskPromise = promise[T]()

    // Construct runnable that completes the promise
    executor.foreach(_.execute(new Runnable {
      override def run(): Unit =
        try {
          val result = taskFunction
          taskPromise.success(result)
        } catch {
          case ex: Throwable => taskPromise.tryFailure(ex)
        }
    }))

    taskPromise.future
  }

  /**
   * Returns the count of tasks including the currently-running one.
   *
   * @return The count of tasks
   */
  def size: Int = taskQueue.size()

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
  def start(): Unit = executor = Some(new ThreadPoolExecutor(
    initialWorkers,
    maxWorkers,
    keepAliveTime,
    TimeUnit.MILLISECONDS,
    taskQueue,
    taskManagerThreadFactory
  ))

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

  /** The default number of workers to spawn initially. */
  val DefaultInitialWorkers = 1

  /** The default maximum number of workers to spawn. */
  val DefaultMaxWorkers = Runtime.getRuntime.availableProcessors()

  /** The default timeout in milliseconds for workers waiting for tasks. */
  val DefaultKeepAliveTime = 1000

  /**
   * The default timeout in milliseconds to wait before stopping a thread
   * if it cannot be interrupted.
   */
  val InterruptTimeout = 5000

  /** The maximum time to wait to add a task to the queue in milliseconds. */
  val MaxQueueTimeout = 10000
}