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

package org.apache.toree.boot.layer

import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

/**
 * Decides what a kernel interrupt signal does, independently of how the
 * signal is delivered (see [[StandardHookInitialization]]).
 *
 * The JVM runs each signal handler on a fresh thread, so signals can be
 * handled concurrently; the timestamp bookkeeping is synchronized, but the
 * interrupt/shutdown actions run outside the lock so a slow interrupt cannot
 * block a following shutdown request.
 *
 * @param interrupt Interrupts the code currently being executed
 * @param shutdown Shuts the kernel down
 * @param shutdownWindowMillis Two signals at most this many milliseconds
 *                             apart request a shutdown
 * @param clock Source of the current time in milliseconds
 */
private[layer] class InterruptSignalHandler(
  interrupt: () => Unit,
  shutdown: () => Unit,
  shutdownWindowMillis: Long = InterruptSignalHandler.DefaultShutdownWindowMillis,
  clock: () => Long = () => System.currentTimeMillis()
) {
  private val logger = LoggerFactory.getLogger(this.getClass.getName)

  /** Time of the previous signal; guarded by `this`. */
  private var lastSignalReceived: Option[Long] = None

  /**
   * Handles SIGINT: interrupts the running code, or shuts the kernel down
   * when this is the second signal within the shutdown window.
   */
  def onSignal(): Unit = {
    val now = clock()
    // Record the signal before acting, so a failing or slow interrupt can
    // never prevent a following signal from requesting a shutdown.
    val isDoubleSignal = synchronized {
      val withinWindow = lastSignalReceived.exists(now - _ <= shutdownWindowMillis)
      lastSignalReceived = Some(now)
      withinWindow
    }

    if (isDoubleSignal) {
      logger.info("Shutting down kernel")
      shutdown()
    } else {
      logger.info("Resetting code execution!")
      interruptSafely()
      logger.info("Enter Ctrl-C twice to shutdown!")
    }
  }

  /**
   * Handles the alternate interrupt signal (`--alternate-sigint`), used when
   * the kernel runs in the background and SIGINT does not reach it. It only
   * interrupts; repeated signals never shut the kernel down.
   */
  def onAlternateSignal(): Unit = {
    logger.info("Resetting code execution due to interrupt!")
    interruptSafely()
  }

  /**
   * Exceptions must not escape: they would only kill the JVM's signal
   * thread, and the user gets no feedback beyond the log.
   */
  private def interruptSafely(): Unit =
    try interrupt()
    catch {
      case NonFatal(ex) => logger.error("Failed to interrupt code execution", ex)
    }
}

private[layer] object InterruptSignalHandler {
  /** Default window in which a second SIGINT requests a shutdown. */
  val DefaultShutdownWindowMillis: Long = 3000
}
