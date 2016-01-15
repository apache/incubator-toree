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

import java.io.{OutputStream, InputStream, File, FileOutputStream}

import org.apache.commons.exec._
import org.apache.commons.exec.environment.EnvironmentUtils
import org.apache.commons.io.{FilenameUtils, IOUtils}
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters._

/**
 * Represents the process used to evaluate broker code.
 *
 * @param processName The name of the process to invoke
 * @param entryResource The resource to be copied and fed as the first argument
 *                      to the process
 * @param otherResources Other resources to be included in the same directory
 *                       as the main resource
 * @param brokerBridge The bridge to use to retrieve kernel output streams
 *                      and the Spark version to be verified
 * @param brokerProcessHandler The handler to use when the process fails or
 *                             completes
 * @param arguments The collection of additional arguments to pass to the
 *                  process after the main entrypoint
 */
class BrokerProcess(
  private val processName: String,
  private val entryResource: String,
  private val otherResources: Seq[String],
  private val brokerBridge: BrokerBridge,
  private val brokerProcessHandler: BrokerProcessHandler,
  private val arguments: Seq[String] = Nil
) extends BrokerName {
  require(processName != null && processName.trim.nonEmpty,
    "Process name cannot be null or pure whitespace!")
  require(entryResource != null && entryResource.trim.nonEmpty,
    "Entry resource cannot be null or pure whitespace!")

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val classLoader = this.getClass.getClassLoader
  private val outputDir =
    s"kernel-$brokerName-" + java.util.UUID.randomUUID().toString

  /** Represents the current process being executed. */
  @volatile private[broker] var currentExecutor: Option[Executor] = None

  /**
   * Returns the temporary directory to place any files needed for the process.
   *
   * @return The directory path as a string
   */
  protected def getTmpDirectory: String = System.getProperty("java.io.tmpdir")

  /**
   * Returns the subdirectory to use to place any files needed for the process.
   *
   * @return The directory path as a string
   */
  protected lazy val getSubDirectory: String =
    s"kernel-$brokerName-" + java.util.UUID.randomUUID().toString

  /**
   * Copies a resource from an input stream to an output stream.
   *
   * @param inputStream The input stream to copy from
   * @param outputStream The output stream to copy to
   *
   * @return The result of the copy operation
   */
  protected def copy(inputStream: InputStream, outputStream: OutputStream) =
    IOUtils.copy(inputStream, outputStream)

  /**
   * Copies a file from the kernel resources to the temporary directory.
   *
   * @param resource The resource to copy
   *
   * @return The string path pointing to the resource's destination
   */
  protected def copyResourceToTmp(resource: String): String = {
    val brokerRunnerResourceStream = classLoader.getResourceAsStream(resource)

    val tmpDirectory = Option(getTmpDirectory)
      .getOrElse(throw new BrokerException("java.io.tmpdir is not set!"))
    val subDirectory = Option(getSubDirectory).getOrElse("")
    val outputName = FilenameUtils.getName(resource)

    val outputDir = Seq(tmpDirectory, subDirectory)
      .filter(_.trim.nonEmpty).mkString("/")
    val outputScript = new File(FilenameUtils.concat(outputDir, outputName))

    // If our script destination is a directory, we cannot copy the script
    if (outputScript.exists() && outputScript.isDirectory)
      throw new BrokerException(s"Failed to create script: $outputScript")

    // Ensure that all of the directories leading up to the script exist
    val outputDirFile = new File(outputDir)
    if (!outputDirFile.exists()) outputDirFile.mkdirs()

    // Copy the script to the specified temporary destination
    val outputScriptStream = new FileOutputStream(outputScript)
    copy(
      brokerRunnerResourceStream,
      outputScriptStream
    )
    outputScriptStream.close()

    // Return the destination of the script
    val destination = outputScript.getPath
    logger.debug(s"Successfully copied $resource to $destination")
    destination
  }

  /**
   * Creates a new process environment to be used for environment variable
   * retrieval by the new process.
   *
   * @return The map of environment variables and their respective values
   */
  protected def newProcessEnvironment(): Map[String, String] = {
    val procEnvironment = EnvironmentUtils.getProcEnvironment

    procEnvironment.asScala.toMap
  }

  /**
   * Creates a new executor to be used to launch the process.
   *
   * @return The executor to start and manage the process
   */
  protected def newExecutor(): Executor = new DefaultExecutor

  /**
   * Starts the Broker process.
   */
  def start(): Unit = currentExecutor.synchronized {
    assert(currentExecutor.isEmpty, "Process has already been started!")

    val capitalizedBrokerName = brokerName.capitalize

    val script = copyResourceToTmp(entryResource)
    logger.debug(s"New $brokerName script created: $script")

    val createdResources = otherResources.map(copyResourceToTmp)

    // Verify that all files were successfully created
    val createdResult = (script +: createdResources).map(new File(_)).map(f => {
      if (f.exists()) true
      else {
        val resource = f.getPath
        logger.warn(s"Failed to create resource: $resource")
        false
      }
    }).forall(_ == true)
    if (!createdResult) throw new BrokerException(
      s"Failed to create resources for $capitalizedBrokerName"
    )

    val commandLine = CommandLine
      .parse(processName)
      .addArgument(script)
    arguments.foreach(commandLine.addArgument)

    logger.debug(s"$capitalizedBrokerName command: ${commandLine.toString}")

    val executor = newExecutor()

    // TODO: Figure out how to dynamically update the output stream used
    //       to use kernel.out, kernel.err, and kernel.in
    // NOTE: Currently mapping to standard output/input, which will be caught
    //       by our system and redirected through the kernel to the client
    executor.setStreamHandler(new PumpStreamHandler(
      System.out,
      System.err,
      System.in
    ))

    // Marking exit status of 1 as successful exit
    executor.setExitValue(1)

    // Prevent the runner from being killed due to run time as it is a
    // long-term process
    executor.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT))

    val processEnvironment = newProcessEnvironment().asJava
    logger.debug(s"$capitalizedBrokerName environment: $processEnvironment")

    // Start the process using the environment provided to the parent
    executor.execute(commandLine, processEnvironment, brokerProcessHandler)

    currentExecutor = Some(executor)
  }

  /**
   * Stops the Broker process.
   */
  def stop(): Unit = currentExecutor.synchronized {
    currentExecutor.foreach(executor => {
      logger.debug(s"Stopping $brokerName process")
      executor.getWatchdog.destroyProcess()
    })
    currentExecutor = None
  }
}
