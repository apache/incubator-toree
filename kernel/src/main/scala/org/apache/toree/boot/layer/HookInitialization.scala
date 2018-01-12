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

import org.apache.toree.boot.KernelBootstrap
import org.apache.toree.interpreter.Interpreter
import org.apache.toree.utils.LogLike

import com.typesafe.config.Config

/**
 * Represents the hook (interrupt/shutdown) initialization. All JVM-related
 * hooks should be constructed here.
 */
trait HookInitialization {
  /**
   * Initializes and registers all hooks except shutdown.
   *
   * @param config The main config
   * @param interpreter The main interpreter
   */
  def initializeHooks(config: Config, interpreter: Interpreter): Unit

  /**
   * Initializes the shutdown hook.
   */
  def initializeShutdownHook(): Unit
}

/**
 * Represents the standard implementation of HookInitialization.
 */
trait StandardHookInitialization extends HookInitialization {
  this: KernelBootstrap with LogLike =>

  /**
   * Initializes and registers all hooks.
   *
   * @param config The main config
   * @param interpreter The main interpreter
   */
  def initializeHooks(config: Config, interpreter: Interpreter): Unit = {
    registerInterruptHook(config, interpreter)
  }

  /**
   * Initializes the shutdown hook.
   */
  def initializeShutdownHook(): Unit = {
    registerShutdownHook()
  }

  private def registerInterruptHook(config: Config, interpreter: Interpreter): Unit = {
    val self = this

    import sun.misc.{Signal, SignalHandler}

    // TODO: Signals are not a good way to handle this since JVM only has the
    // proprietary sun API that is not necessarily available on all platforms
    Signal.handle(new Signal("INT"), new SignalHandler() {
      private val MaxSignalTime: Long = 3000 // 3 seconds
      var lastSignalReceived: Long    = 0

      def handle(sig: Signal) = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSignalReceived > MaxSignalTime) {
          logger.info("Resetting code execution!")
          interpreter.interrupt()

          // TODO: Cancel group representing current code execution
          //sparkContext.cancelJobGroup()

          logger.info("Enter Ctrl-C twice to shutdown!")
          lastSignalReceived = currentTime
        } else {
          logger.info("Shutting down kernel")
          self.shutdown()
        }
      }
    })
    // Define handler for alternate signal that will be fired in cases where
    // the caller is in a background process in order to interrupt
    // cell operations - since SIGINT doesn't propagate in those cases.
    // Like INT above except we don't need to deal with shutdown in
    // repeated situations.
    val altSigintOption = "alternate_sigint"
    if (config.hasPath(altSigintOption)) {
      val altSigint = config.getString(altSigintOption)

      try {
        Signal.handle(new Signal(altSigint), new SignalHandler() {

            def handle(sig: Signal) = {
              logger.info("Resetting code execution due to interrupt!")
              interpreter.interrupt()

              // TODO: Cancel group representing current code execution
              //sparkContext.cancelJobGroup()
            }
        })
      } catch {
        case e:Exception => logger.warn("Error occurred establishing alternate signal handler " +
          "(--alternate-sigint = " + altSigint + ").  Error: " + e.getMessage )
      }
    }
  }

  private def registerShutdownHook(): Unit = {
    logger.debug("Registering shutdown hook")
    val self = this
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() = {
        logger.info("Shutting down kernel")
        self.shutdown()
      }
    })
  }
}
