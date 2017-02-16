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

import java.net.URL

import org.apache.toree.interpreter.Results.Result
import org.apache.toree.interpreter._
import org.apache.toree.kernel.api.KernelLike
import org.slf4j.LoggerFactory
import py4j.GatewayServer

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.tools.nsc.interpreter.{InputStream, OutputStream}

/**
 * Represents an interpreter interface to PySpark. Requires a properly-set
 * SPARK_HOME, PYTHONPATH pointing to Spark's Python source, and py4j installed
 * where it is accessible to the Spark Kernel.  Optionally specify PYTHON_EXEC
 * to override the default python executable "python'
 *
 */
class PySparkInterpreter(
) extends Interpreter {
  /** Maximum time to wait for the python kernel to be readu */
  private val WAIT_DURATION: Long = java.util.concurrent.TimeUnit.SECONDS.toMillis(50)

  private val PythonExecEnv = "PYTHON_EXEC"
  private lazy val pythonExecutable = Option(System.getenv(PythonExecEnv)).getOrElse("python")
  private val logger = LoggerFactory.getLogger(this.getClass)
  private var _kernel:KernelLike = _

  // TODO: Replace hard-coded maximum queue count
  /** Represents the state used by this interpreter's Python instance. */
  private lazy val pySparkState = new PySparkState(500)

  /** Represents the bridge used by this interpreter's Python interface. */
  private lazy val pySparkBridge = PySparkBridge(
    pySparkState,
    _kernel
  )


  /** Represents the interface for Python to talk to JVM Spark components. */
  private lazy val gatewayServer = new GatewayServer(pySparkBridge, 0)

  /** Represents the process handler used for the PySpark process. */
  private lazy val pySparkProcessHandler: PySparkProcessHandler =
    new PySparkProcessHandler(
      pySparkBridge,
      restartOnFailure = true,
      restartOnCompletion = true
    )

  private lazy val pySparkService = new PySparkService(
    pythonExecutable,
    gatewayServer,
    pySparkBridge,
    pySparkProcessHandler
  )
  private lazy val pySparkTransformer = new PySparkTransformer

  /**
   * Initializes the interpreter.
    *
    * @param kernel The kernel
   * @return The newly initialized interpreter
   */
  override def init(kernel: KernelLike): Interpreter = {
    _kernel = kernel
    this
  }

  /**
   * Executes the provided code with the option to silence output.
    *
    * @param code The code to execute
   * @param silent Whether or not to execute the code silently (no output)
   * @return The success/failure of the interpretation and the output from the
   *         execution or the failure
   */
  override def interpret(code: String, silent: Boolean, output: Option[OutputStream]):
    (Result, Either[ExecuteOutput, ExecuteFailure]) = {
    if (!pySparkService.isRunning) pySparkService.start()

    val futureResult = pySparkTransformer.transformToInterpreterResult(
      pySparkService.submitCode(code, output)
    )

    Await.result(futureResult, Duration.Inf)
  }

  /**
   * Starts the interpreter, initializing any internal state.
    *
    * @return A reference to the interpreter
   */
  override def start(): Interpreter = {
    pySparkService.start()

    this
  }

  /**
   * Stops the interpreter, removing any previous internal state.
    *
    * @return A reference to the interpreter
   */
  override def stop(): Interpreter = {
    pySparkService.stop()

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

  override def languageInfo: LanguageInfo = {
    import scala.sys.process._

    // Issue a subprocess call to grab the python version.  This is better than polling a child process.
    val version = Seq(
      pythonExecutable,
      "-c",
      "import sys; print('{s.major}.{s.minor}.{s.micro}'.format(s=sys.version_info))").!!

    LanguageInfo(
      "python",
      version = version,
      fileExtension = Some(".py"),
      pygmentsLexer = Some("python"),
      mimeType = Some("text/x-ipython"),
      codemirrorMode = Some("text/x-ipython"))
  }
}
