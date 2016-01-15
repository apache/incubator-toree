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

package org.apache.toree.interpreter.broker

import java.util.concurrent.ConcurrentHashMap

import org.apache.toree.interpreter.broker.BrokerTypes._
import org.slf4j.LoggerFactory

import scala.concurrent.{Future, promise}

/**
 * Represents the state structure of broker.
 *
 * @param maxQueuedCode The maximum amount of code to support being queued
 *                      at the same time for broker execution
 *
 */
class BrokerState(private val maxQueuedCode: Int) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  import scala.collection.JavaConverters._

  private var _isReady: Boolean = false
  protected val codeQueue: java.util.Queue[BrokerCode] =
    new java.util.concurrent.ConcurrentLinkedQueue[BrokerCode]()
  protected val promiseMap: collection.mutable.Map[CodeId, BrokerPromise] =
    new ConcurrentHashMap[CodeId, BrokerPromise]().asScala

  /**
   * Adds new code to eventually be executed.
   *
   * @param code The snippet of code to execute
   *
   * @return The future containing the results of the execution
   */
  def pushCode(code: Code): Future[CodeResults] = synchronized {
    // Throw the standard error if our maximum limit has been reached
    if (codeQueue.size() >= maxQueuedCode)
      throw new IllegalStateException(
        s"Code limit of $maxQueuedCode has been reached!")

    // Generate our promise that will be fulfilled when the code is executed
    // and the results are sent back
    val codeExecutionPromise = promise[CodeResults]()

    // Build the code representation to send to Broker
    val uniqueId = java.util.UUID.randomUUID().toString
    val brokerCode = BrokerCode(uniqueId, code)
    val brokerPromise = BrokerPromise(uniqueId, codeExecutionPromise)

    logger.debug(s"Queueing '$code' with id '$uniqueId' to run with broker")

    // Add the code to be executed to our queue and the promise to our map
    codeQueue.add(brokerCode)
    promiseMap.put(brokerPromise.codeId, brokerPromise)

    codeExecutionPromise.future
  }

  /**
   * Returns the total code currently queued to be executed.
   *
   * @return The total number of code instances queued to be executed
   */
  def totalQueuedCode(): Int = codeQueue.size()

  /**
   * Retrieves (and removes) the next piece of code to be executed.
   *
   * @note This should only be invoked by the broker process!
   *
   * @return The next code to execute if available, otherwise null
   */
  def nextCode(): BrokerCode = {
    val brokerCode = codeQueue.poll()

    if (brokerCode != null)
      logger.trace(s"Sending $brokerCode to Broker runner")

    brokerCode
  }

  /**
   * Indicates whether or not the broker instance is ready for code.
   *
   * @return True if it is ready, otherwise false
   */
  def isReady: Boolean = _isReady

  /**
   * Marks the state of broker as ready.
   */
  def markReady(): Unit = _isReady = true

  /**
   * Marks the specified code as successfully completed using its id.
   *
   * @param codeId The id of the code to mark as a success
   * @param output The output from the execution to be used as the result
   */
  def markSuccess(codeId: CodeId, output: CodeResults): Unit = {
    logger.debug(s"Received success for code with id '$codeId': $output")
    promiseMap.remove(codeId).foreach(_.promise.success(output))
  }

  /**
   * Marks the specified code as successfully completed using its id. Output
   * from success is treated as an empty string.
   *
   * @param codeId The id of the code to mark as a success
   */
  def markSuccess(codeId: CodeId): Unit = markSuccess(codeId, "")

  /**
   * Marks the specified code as unsuccessful using its id.
   *
   * @param codeId The id of the code to mark as a failure
   * @param output The output from the error to be used as the description
   *               of the exception
   */
  def markFailure(codeId: CodeId, output: CodeResults): Unit = {
    logger.debug(s"Received failure for code with id '$codeId': $output")
    promiseMap.remove(codeId).foreach(
      _.promise.failure(new BrokerException(output)))
  }

  /**
   * Marks the specified code as unsuccessful using its id. Output from failure
   * is treated as an empty string.
   *
   * @param codeId The id of the code to mark as a failure
   */
  def markFailure(codeId: CodeId): Unit = markFailure(codeId, "")

  /**
   * Resets the state by clearing any pending code executions and marking all
   * pending executions as failures (or success if specified).
   *
   * @param message The message to present through the interrupted promises
   * @param markAllAsFailure If true, marks all pending executions as failures,
   *                         otherwise marks all as success
   */
  def reset(message: String, markAllAsFailure: Boolean = true): Unit = {
    codeQueue.synchronized {
      promiseMap.synchronized {
        codeQueue.clear()

        // Use map contents for reset as it should contain non-executing
        // code as well as executing code
        promiseMap.foreach { case (codeId, codePromise) =>
          if (markAllAsFailure)
            codePromise.promise.failure(new BrokerException(message))
          else
            codePromise.promise.success(message)
        }
        promiseMap.clear()
      }
    }
  }
}

