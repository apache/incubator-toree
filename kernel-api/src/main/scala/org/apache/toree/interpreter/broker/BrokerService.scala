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

package org.apache.toree.interpreter.broker

import org.apache.toree.interpreter.broker.BrokerTypes.{Code, CodeResults}

import scala.concurrent.Future
import scala.tools.nsc.interpreter._

/**
 * Represents the service that provides the high-level interface between the
 * JVM and another process.
 */
trait BrokerService {
  /** Starts the broker service. */
  def start(): Unit

  /**
   * Indicates whether or not the service is running.
   *
   * @return True if running, otherwise false
   */
  def isRunning: Boolean

  /**
   * Submits code to the broker service to be executed and return a result.
   *
   * @param code The code to execute
   * @param kernelOutputStream The output stream to write to
   *
   * @return The result as a future to eventually return
   */
  def submitCode(code: Code, kernelOutputStream: Option[OutputStream]): Future[CodeResults]

  /** Stops the running broker service. */
  def stop(): Unit
}
