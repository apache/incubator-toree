/*
 * Copyright 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.toree.kernel.interpreter.pyspark

import org.apache.toree.interpreter.broker.BrokerService
import org.apache.toree.kernel.interpreter.pyspark.PySparkTypes._
import org.apache.spark.SparkContext
import org.slf4j.LoggerFactory
import py4j.GatewayServer

import scala.concurrent.Future

/**
 * Represents the service that provides the high-level interface between the
 * JVM and Python.
 *
 * @param gatewayServer The backend to start to communicate between the JVM and
 *                      Python
 * @param pySparkBridge The bridge to use for communication between the JVM and
 *                      Python
 * @param pySparkProcessHandler The handler used for events that occur with
 *                              the PySpark process
 */
class PySparkService(
  private val gatewayServer: GatewayServer,
  private val pySparkBridge: PySparkBridge,
  private val pySparkProcessHandler: PySparkProcessHandler
) extends BrokerService {
  private val logger = LoggerFactory.getLogger(this.getClass)
  @volatile private var _isRunning: Boolean = false
  override def isRunning: Boolean = _isRunning


  /** Represents the process used to execute Python code via the bridge. */
  private lazy val pySparkProcess = {
    val p = new PySparkProcess(
      pySparkBridge,
      pySparkProcessHandler,
      gatewayServer.getListeningPort,
      org.apache.spark.SPARK_VERSION
    )

    // Update handlers to correctly reset and restart the process
    pySparkProcessHandler.setResetMethod(message => {
      p.stop()
      pySparkBridge.state.reset(message)
    })
    pySparkProcessHandler.setRestartMethod(() => p.start())

    p
  }

  /** Starts the PySpark service. */
  def start(): Unit = {
    // Start without forking the gateway server (needs to have access to
    // SparkContext in current JVM)
    logger.debug("Starting gateway server")
    gatewayServer.start()

    val port = gatewayServer.getListeningPort
    logger.debug(s"Gateway server running on port $port")

    // Start the Python process used to execute code
    logger.debug("Launching process to execute Python code")
    pySparkProcess.start()

    _isRunning = true
  }

  /**
   * Submits code to the PySpark service to be executed and return a result.
   *
   * @param code The code to execute
   *
   * @return The result as a future to eventually return
   */
  def submitCode(code: Code): Future[CodeResults] = {
    pySparkBridge.state.pushCode(code)
  }

  /** Stops the running PySpark service. */
  def stop(): Unit = {
    // Stop the Python process used to execute code
    pySparkProcess.stop()

    // Stop the server used as an entrypoint for Python
    gatewayServer.shutdown()

    _isRunning = false
  }
}
