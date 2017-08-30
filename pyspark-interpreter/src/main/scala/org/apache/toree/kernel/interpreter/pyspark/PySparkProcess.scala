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
package org.apache.toree.kernel.interpreter.pyspark

import java.io.{FileOutputStream, File}

import org.apache.toree.interpreter.broker.BrokerProcess
import org.apache.commons.exec.environment.EnvironmentUtils
import org.apache.commons.exec._
import org.apache.commons.io.IOUtils
import org.apache.spark.SparkContext
import org.slf4j.LoggerFactory
import sys.process._

/**
 * Represents the Python process used to evaluate PySpark code.
 *
 * @param pythonProcessName name of python process
 * @param pySparkBridge The bridge to use to retrieve kernel output streams
 *                      and the Spark version to be verified
 * @param pySparkProcessHandler The handler to use when the process fails or
 *                              completes
 * @param port The port to provide to the PySpark process to use to connect
 *             back to the JVM
 * @param sparkVersion The version of Spark that the process will be using
 */
class PySparkProcess(
  private val pythonProcessName: String,
  private val pySparkBridge: PySparkBridge,
  private val pySparkProcessHandler: PySparkProcessHandler,
  private val port: Int,
  private val sparkVersion: String
) extends BrokerProcess(
  processName = pythonProcessName,
  entryResource = "PySpark/pyspark_runner.py",
  otherResources = Nil,
  brokerBridge = pySparkBridge,
  brokerProcessHandler = pySparkProcessHandler,
  arguments = Seq(port.toString, sparkVersion)
) {

  override val brokerName: String = "PySpark"
  private val logger = LoggerFactory.getLogger(this.getClass)

  private val sparkHome = Option(System.getenv("SPARK_HOME"))
    .orElse(Option(System.getProperty("spark.home")))
  private val pythonPath = Option(System.getenv("PYTHONPATH"))

  assert(sparkHome.nonEmpty, "PySpark process requires Spark Home to be set!")
  if (pythonPath.isEmpty) logger.warn("PYTHONPATH not provided for PySpark!")

  /**
   * Creates a new process environment to be used for environment variable
   * retrieval by the new process.
   *
   * @return The map of environment variables and their respective values
   */
  override protected def newProcessEnvironment(): Map[String, String] = {
    val baseEnvironment = super.newProcessEnvironment()

    import java.io.File.pathSeparator

    val baseSparkHome = sparkHome.get
    val basePythonPath = pythonPath.getOrElse("")
    val updatedPythonPath =
      (basePythonPath.split(pathSeparator) :+ s"$baseSparkHome/python/")
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(new File(_))
        .distinct
        .mkString(pathSeparator)

    // Note: Adding the new map values should override the old ones
    baseEnvironment ++ Map(
      "SPARK_HOME" -> baseSparkHome,
      "PYTHONPATH" -> updatedPythonPath
    )
  }

  override protected def copyResourceToTmp(resource: String): String = {
    val destination = super.copyResourceToTmp(resource)
    if (System.getProperty("os.name").equals("z/OS")){
        tagPySparkResource(destination)
    }
    destination 
  }

  private def tagPySparkResource(destPath: String): Unit = {
      val exitCode = Seq("chtag", "-t", "-c", "ISO8859-1", destPath).!
      if (exitCode != 0) logger.warn("PySpark resource was not tagged correctly.")
  }
}
