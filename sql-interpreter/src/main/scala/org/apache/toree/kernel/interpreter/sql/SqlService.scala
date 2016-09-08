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
package org.apache.toree.kernel.interpreter.sql

import java.io.ByteArrayOutputStream

import org.apache.toree.interpreter.broker.BrokerService
import org.apache.toree.kernel.api.KernelLike
import org.apache.toree.kernel.interpreter.sql.SqlTypes._

import scala.concurrent.Future
import scala.tools.nsc.interpreter._

/**
 * Represents the service that provides the high-level interface between the
 * JVM and Spark SQL.
 *
 * @param kernel The SQL Context of Apache Spark to use to perform SQL
 *                   queries
 */
class SqlService(private val kernel: KernelLike) extends BrokerService {
  import scala.concurrent.ExecutionContext.Implicits.global

  @volatile private var _isRunning: Boolean = false
  override def isRunning: Boolean = _isRunning

  /**
   * Submits code to the broker service to be executed and return a result.
   *
   * @param code The code to execute
   *
   * @return The result as a future to eventually return
   */
  override def submitCode(code: Code, kernelOutputStream: Option[OutputStream]): Future[CodeResults] = Future {
    println(s"Executing: '${code.trim}'")
    val result = kernel.sparkSession.sql(code.trim)

    // TODO: There is an internal method used for show called showString that
    //       supposedly is only for the Python API, look into why
    val stringOutput = {
      val outputStream = new ByteArrayOutputStream()
      Console.withOut(outputStream) {
        // TODO: Provide some way to change the number of records shown
        result.show(10)
      }
      outputStream.toString("UTF-8")
    }

    stringOutput
  }

  /** Stops the running broker service. */
  override def stop(): Unit = _isRunning = false

  /** Starts the broker service. */
  override def start(): Unit = _isRunning = true
}
