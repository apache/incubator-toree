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

import org.apache.toree.interpreter.broker.BrokerProcess
import scala.collection.JavaConverters._

/**
 * Represents the R process used to evaluate SparkR code.
 *
 * @param processName The name of the Rscript process to run.
 * @param sparkRBridge The bridge to use to retrieve kernel output streams
 *                      and the Spark version to be verified
 * @param sparkRProcessHandler The handler to use when the process fails or
 *                             completes
 * @param port The port to provide to the SparkR process to use to connect
 *             back to the JVM
 */
class SparkRProcess(
  processName: String,
  private val sparkRBridge: SparkRBridge,
  private val sparkRProcessHandler: SparkRProcessHandler,
  private val port: Int
) extends BrokerProcess(
  processName = processName,
  entryResource = "kernelR/sparkr_runner.R",
  otherResources = Seq("kernelR/sparkr_runner_utils.R"),
  brokerBridge = sparkRBridge,
  brokerProcessHandler = sparkRProcessHandler,
  arguments = Seq(
    "--default-packages=datasets,utils,grDevices,graphics,stats,methods"
  )
) {
  override val brokerName: String = "SparkR"
  private val sparkHome = Option(System.getenv("SPARK_HOME"))
    .orElse(Option(System.getProperty("spark.home")))

  assert(sparkHome.nonEmpty, "SparkR process requires Spark Home to be set!")

  /**
   * Creates a new process environment to be used for environment variable
   * retrieval by the new process.
   *
   * @return The map of environment variables and their respective values
   */
  override protected def newProcessEnvironment(): Map[String, String] = {
    val baseEnvironment = super.newProcessEnvironment()

    // Note: Adding the new map values should override the old ones
    baseEnvironment ++ Map(
      "SPARK_HOME"                    -> sparkHome.get,
      "EXISTING_SPARKR_BACKEND_PORT"  -> port.toString
    )
  }
}
