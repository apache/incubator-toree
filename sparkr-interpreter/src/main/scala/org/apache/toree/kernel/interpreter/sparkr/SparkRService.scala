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
package org.apache.toree.kernel.interpreter.sparkr

import java.util.concurrent.{Semaphore, TimeUnit}

import org.apache.toree.interpreter.broker.BrokerService
import org.apache.toree.kernel.interpreter.sparkr.SparkRTypes.{Code, CodeResults}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.tools.nsc.interpreter._

/**
 * Represents the service that provides the high-level interface between the
 * JVM and R.
 *
 * @param rBackend The backend to start to communicate between the JVM and R
 * @param sparkRBridge The bridge to use for communication between the JVM and R
 * @param sparkRProcessHandler The handler used for events that occur with the
 *                             SparkR process
 */
class SparkRService(
  private val rBackend: ReflectiveRBackend,
  private val sparkRBridge: SparkRBridge,
  private val sparkRProcessHandler: SparkRProcessHandler
) extends BrokerService {
  private val logger = LoggerFactory.getLogger(this.getClass)
  @volatile private var rBackendPort: Int = -1
  @volatile private var _isRunning: Boolean = false
  override def isRunning: Boolean = _isRunning

  /** Represents the process used to execute R code via the bridge. */
  private lazy val sparkRProcess: SparkRProcess = {
    val p = new SparkRProcess(
      sparkRBridge,
      sparkRProcessHandler,
      rBackendPort
    )

    // Update handlers to correctly reset and restart the process
    sparkRProcessHandler.setResetMethod(message => {
      p.stop()
      sparkRBridge.state.reset(message)
    })
    sparkRProcessHandler.setRestartMethod(() => p.start())

    p
  }

  /** Starts the SparkR service. */
  override def start(): Unit = {
    logger.debug("Initializing statically-accessible SparkR bridge")
    SparkRBridge.sparkRBridge = sparkRBridge

    val initialized = new Semaphore(0)
    val classLoader = SparkRBridge.getClass.getClassLoader
    import scala.concurrent.ExecutionContext.Implicits.global
    val rBackendRun = Future {
      logger.debug("Initializing RBackend")
      rBackendPort = rBackend.init(classLoader)
      logger.debug(s"RBackend running on port $rBackendPort")
      initialized.release()
      logger.debug("Running RBackend")
      rBackend.run()
      logger.debug("RBackend has finished")
    }

    // Wait for backend to start before starting R process to connect
    val backendTimeout =
      sys.env.getOrElse("SPARKR_BACKEND_TIMEOUT", "120").toInt
    if (initialized.tryAcquire(backendTimeout, TimeUnit.SECONDS)) {
      // Start the R process used to execute code
      logger.debug("Launching process to execute R code")
      sparkRProcess.start()
      _isRunning = true
    } else {
      // Unable to initialize, so throw an exception
      throw new SparkRException(
        s"Unable to initialize R backend in $backendTimeout seconds!")
    }
  }

  /**
   * Submits code to the SparkR service to be executed and return a result.
   *
   * @param code The code to execute
   *
   * @return The result as a future to eventually return
   */
  override def submitCode(code: Code, kernelOutputStream: Option[OutputStream]): Future[CodeResults] = {
    sparkRBridge.state.pushCode(code, kernelOutputStream)
  }

  /** Stops the running SparkR service. */
  override def stop(): Unit = {
    // Stop the R process used to execute code
    sparkRProcess.stop()

    // Stop the server used as an entrypoint for R
    rBackend.close()

    // Clear the bridge
    SparkRBridge.reset()

    _isRunning = false
  }
}
