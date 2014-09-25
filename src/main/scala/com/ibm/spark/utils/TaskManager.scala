package com.ibm.spark.utils

import java.util.concurrent.{TimeUnit, ArrayBlockingQueue}
import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.{Future, Promise, promise}

/**
 * Represents a generic manager of Runnable tasks that will be executed in a
 * separate thread (created inside the manager). Facilitates running tasks and
 * provides a method
 */
class TaskManager extends LogLike{
  // Maximum time to wait (in milliseconds) before forcefully stopping this
  // thread when an interrupt fails
  private val InterruptTimeout = 5 * 1000
  private val _queueCapacity = 200
  private val _taskQueue = new ArrayBlockingQueue[(Runnable, Promise[_])](_queueCapacity)
  private var _taskThread: TaskThread = _

  private val _currentPromise: AtomicReference[Promise[_]] =
    new AtomicReference[Promise[_]]()

  private class TaskThread extends Thread {
    private[TaskManager] var _currentTask: Runnable = _
    private[TaskManager] var _running = false

    override def start(): Unit = {
      _running = true
      super.start()
    }

    /**
     * Main execution loop of the task thread.
     *
     * Pulls tasks from an internal queue to be processed sequentially.
     */
    override def run(): Unit = {
      while (_running) {
        val element = _taskQueue.poll(1L, TimeUnit.MILLISECONDS)
        if (element != null) {
          _currentTask = element._1
          _currentPromise.set(element._2)

          if (_currentTask != null) _currentTask.run()

          _currentTask = null
          _currentPromise.set(null)
        }
      }
    }

    /**
     * Marks the internal flag for running new tasks to false.
     */
    def cancel(): Unit = {
      _running = false
    }
  }

  /**
   * Represents the internal thread used to process tasks.
   *
   * @return The Thread instance wrapped in an Option, or None if not started
   */
  def thread: Option[Thread] =
    if (_taskThread != null) Some(_taskThread) else None

  /**
   * Adds a new task to the list to execute.
   *
   * @param taskFunction The new task as a block of code
   *
   * @return Future representing the return value (or error) from the task
   */
  def add[T <: Any](taskFunction: => T): Future[T] = {
    val taskPromise = promise[T]()

    // Construct runnable that completes the promise
    _taskQueue.add((new Runnable {
      override def run(): Unit =
        try {
          val result = taskFunction
          taskPromise.success(result)
        } catch {
          case ex: Throwable => taskPromise.tryFailure(ex)
        }
    }, taskPromise))

    taskPromise.future
  }

  /**
   * Returns the count of tasks including the currently-running one.
   *
   * @return The count of tasks
   */
  def size: Int = _taskQueue.size()

  /**
   * Returns whether or not there is a task in the queue to be processed.
   *
   * @return True if the internal queue is not empty, otherwise false
   */
  def hasTaskInQueue: Boolean = !_taskQueue.isEmpty

  /**
   * Returns the current executing task.
   *
   * @return The current task or None if no task is running
   */
  def currentTask: Option[Runnable] =
    if (_taskThread != null && _taskThread._currentTask != null)
      Some(_taskThread._currentTask)
    else None

  /**
   * Returns the sequence of tasks.
   *
   * @return The sequence of tasks as Runnables
   */
  def tasks: Seq[Runnable] = _taskQueue.toArray.map(_.asInstanceOf[(Runnable, _)]._1)

  /**
   * Whether or not there is a task being executed currently.
   *
   * @return True if there is a task being executed, otherwise false
   */
  def isExecutingTask: Boolean = currentTask.nonEmpty

  /**
   * Whether or not the task manager is processing new tasks.
   *
   * @return True if the manager is capable of consuming more tasks,
   *         otherwise false
   */
  def isRunning: Boolean = _taskThread != null && _taskThread._running

  /**
   * Block execution (by sleeping) until all tasks currently queued up for
   * execution are processed.
   */
  def await(): Unit =
    while (hasTaskInQueue || isExecutingTask) Thread.sleep(1)

  /**
   * Starts the task manager (begins processing tasks). Creates a new thread
   * in the process.
   */
  def start(): Unit = startTaskProcessingThread()

  /**
   * Restarts internal processing of tasks (removing current task).
   */
  def restart(): Unit = restartTaskProcessingThread()

  /**
   * Stops internal processing of tasks.
   *
   * @param killThread Whether to kill the thread processing tasks if not it
   *                   is not responding to interrupts
   * @param killTimeout The period of time (in milliseconds) to wait before
   *                    attempting to kill the thread processing tasks
   */
  def stop(
    killThread: Boolean = true,
    killTimeout: Int = InterruptTimeout
  ): Unit = killTaskProcessingThread(killThread, killTimeout)

  /**
   * Creates a new task-processing thread and starts it.
   */
  private def startTaskProcessingThread(): Unit = {
    _taskThread = new TaskThread
    _taskThread.start()
  }

  /**
   * Attempts to cancel/interrupt the task manager's internal task-processing
   * thread. If unable to interrupt, will be forcefully stopped after the
   * interrupt timeout threshold is reached.
   *
   * @param killThread Whether to kill the thread processing tasks if not it
   *                   is not responding to interrupts
   * @param killTimeout The period of time (in milliseconds) to wait before
   *                    attempting to kill the thread processing tasks
   */
  private def killTaskProcessingThread(
    killThread: Boolean = true,
    killTimeout: Int = InterruptTimeout
  ): Unit = {
    // NOTE: Dirty hack to suppress deprecation warnings
    // See https://issues.scala-lang.org/browse/SI-7934 for discussion
    object Thread {
      @deprecated("", "") class Killer {
        def stop() = try { _taskThread.stop() }
      }

      object Killer extends Killer
    }

    _taskThread.cancel()
    _taskThread.interrupt()

    runIfTimeout(
      killTimeout,
      _taskThread.isAlive && killThread,
      // Still available (JDK8 now calls stop0 directly)
      //
      // Used because of discussion on:
      // https://issues.scala-lang.org/browse/SI-6302
      {
        //_taskThread.stop()
        Thread.Killer.stop()
        val currentPromise = _currentPromise.get()
        if (currentPromise != null)
          currentPromise.tryFailure(new ThreadDeath())
      }
    )

    _taskThread = null
  }

  /**
   * Shuts down the current thread used to execute tasks and starts a new
   * thread in its place.
   */
  private def restartTaskProcessingThread(): Unit = {
    killTaskProcessingThread()
    startTaskProcessingThread()
  }

  /**
   * Execute body if the millisecond timeout is reached and the condition
   * still holds true.
   *
   * @param milliseconds The timeout limit in milliseconds
   * @param condition The condition to test up until executing the body
   * @param body The body to execute
   *
   * @return Whether or not the body was executed
   */
  private def runIfTimeout(
    milliseconds: Int,
    condition: => Boolean,
    body: => Unit
  ): Boolean = {
    val endTime: Long = milliseconds + java.lang.System.currentTimeMillis()

    while (java.lang.System.currentTimeMillis() < endTime) {
      // Exit if our condition suddenly goes south
      if (!condition) return false

      // Allow check to be interrupted
      if (Thread.interrupted())
        throw new InterruptedException()
    }

    if (condition) { body; true } else false
  }
}

