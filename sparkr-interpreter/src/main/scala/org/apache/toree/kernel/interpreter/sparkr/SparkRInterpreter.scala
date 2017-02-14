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

import java.net.URL

import org.apache.toree.interpreter.Results.Result
import org.apache.toree.interpreter._
import org.apache.toree.kernel.api.KernelLike
import org.apache.toree.kernel.BuildInfo
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.tools.nsc.interpreter.{InputStream, OutputStream}

/**
 * Represents an interpreter interface to SparkR. Requires a properly-set
 * SPARK_HOME pointing to a binary distribution (needs packaged SparkR library)
 * and an implementation of R on the path.
 *
 */
class SparkRInterpreter(
) extends Interpreter {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private var _kernel: KernelLike = _
  private val rScriptExecutable = "Rscript"

  // TODO: Replace hard-coded maximum queue count
  /** Represents the state used by this interpreter's R instance. */
  private lazy val sparkRState = new SparkRState(500)

  /** Represents the bridge used by this interpreter's R instance. */
  private lazy val sparkRBridge = SparkRBridge(
    sparkRState,
    _kernel
  )

  /** Represents the interface for R to talk to JVM Spark components. */
  private lazy val rBackend = new ReflectiveRBackend

  /** Represents the process handler used for the SparkR process. */
  private lazy val sparkRProcessHandler: SparkRProcessHandler =
    new SparkRProcessHandler(
      sparkRBridge,
      restartOnFailure = true,
      restartOnCompletion = true
    )

  private lazy val sparkRService = new SparkRService(
    rScriptExecutable,
    rBackend,
    sparkRBridge,
    sparkRProcessHandler
  )
  private lazy val sparkRTransformer = new SparkRTransformer

  override def init(kernel: KernelLike): Interpreter = {
    _kernel = kernel
    this
  }

  /**
   * Executes the provided code with the option to silence output.
   * @param code The code to execute
   * @param silent Whether or not to execute the code silently (no output)
   * @return The success/failure of the interpretation and the output from the
   *         execution or the failure
   */
  override def interpret(code: String, silent: Boolean, output: Option[OutputStream]):
    (Result, Either[ExecuteOutput, ExecuteFailure]) =
  {
    if (!sparkRService.isRunning) sparkRService.start()

    val futureResult = sparkRTransformer.transformToInterpreterResult(
      sparkRService.submitCode(code, kernelOutputStream = output)
    )

    Await.result(futureResult, Duration.Inf)
  }

  /**
   * Starts the interpreter, initializing any internal state.
   * @return A reference to the interpreter
   */
  override def start(): Interpreter = {
    sparkRService.start()

    this
  }

  /**
   * Stops the interpreter, removing any previous internal state.
   * @return A reference to the interpreter
   */
  override def stop(): Interpreter = {
    sparkRService.stop()

    this
  }

  /**
   * Returns the class loader used by this interpreter.
   *
   * @return The runtime class loader used by this interpreter
   */
  override def classLoader: ClassLoader = this.getClass.getClassLoader

  // Unsupported (but can be invoked)
  override def lastExecutionVariableName: Option[String] = None

  // Unsupported (but can be invoked)
  override def read(variableName: String): Option[AnyRef] = None

  // Unsupported
  override def updatePrintStreams(in: InputStream, out: OutputStream, err: OutputStream): Unit = ???

  // Unsupported
  override def interrupt(): Interpreter = ???

  // Unsupported
  override def bind(variableName: String, typeName: String, value: Any, modifiers: List[String]): Unit = ???

  // Unsupported
  override def addJars(jars: URL*): Unit = ???

  // Unsupported
  override def doQuietly[T](body: => T): T = ???

  override def languageInfo = {
    import sys.process._

    // Issue a subprocess call to grab the R version.  This is better than polling a child process.
    val version = Seq(
      rScriptExecutable,
      "-e",
      "cat(R.version$major, '.', R.version$minor, sep='', fill=TRUE)").!!

    LanguageInfo(
      "R", version = version,
      fileExtension = Some(".R"),
      pygmentsLexer = Some("r"),
      mimeType = Some("text/x-rsrc"),
      codemirrorMode = Some("text/x-rsrc"))
  }

}
