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

package org.apache.toree.kernel.api

import java.io.{InputStream, PrintStream}
import java.net.URI
import java.util.concurrent.{ConcurrentHashMap, TimeUnit, TimeoutException}
import scala.collection.mutable
import com.typesafe.config.Config
import org.apache.spark.api.java.JavaSparkContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.toree.annotations.Experimental
import org.apache.toree.boot.layer.InterpreterManager
import org.apache.toree.comm.CommManager
import org.apache.toree.global
import org.apache.toree.global.ExecuteRequestState
import org.apache.toree.interpreter.Results.Result
import org.apache.toree.interpreter._
import org.apache.toree.kernel.protocol.v5
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.kernel.protocol.v5.magic.MagicParser
import org.apache.toree.kernel.protocol.v5.stream.KernelOutputStream
import org.apache.toree.kernel.protocol.v5.{KMBuilder, KernelMessage, MIMEType}
import org.apache.toree.magic.MagicManager
import org.apache.toree.plugins.PluginManager
import org.apache.toree.utils.LogLike
import scala.language.dynamics
import scala.reflect.runtime.universe._
import scala.util.DynamicVariable
import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Await}

/**
 * Represents the main kernel API to be used for interaction.
 *
 * @param _config The configuration used when starting the kernel
 * @param interpreterManager The interpreter manager to expose in this instance
 * @param comm The Comm manager to expose in this instance
 * @param actorLoader The actor loader to use for message relaying
 */
@Experimental
class Kernel (
  private val _config: Config,
  private val actorLoader: ActorLoader,
  val interpreterManager: InterpreterManager,
  val comm: CommManager,
  val pluginManager: PluginManager
) extends KernelLike with LogLike {

  /**
   * Jars that have been added to the kernel
   */
  private val jars = new mutable.ArrayBuffer[URI]()

  override def addJars(uris: URI*): Unit = {
    uris.foreach { uri =>
      if (uri.getScheme != "file") {
        throw new RuntimeException("Cannot add non-local jar: " + uri)
      }
    }

    jars ++= uris
    interpreter.addJars(uris.map(_.toURL):_*)
    uris.foreach(uri => sparkContext.addJar(uri.getPath))
  }

  /**
   * Represents the current input stream used by the kernel for the specific
   * thread.
   */
  private val currentInputStream =
    new DynamicVariable[InputStream](null)
  private val currentInputKernelMessage =
    new DynamicVariable[KernelMessage](null)

  /**
   * Represents the current output stream used by the kernel for the specific
   * thread.
   */
  private val currentOutputStream =
    new DynamicVariable[PrintStream](null)
  private val currentOutputKernelMessage =
    new DynamicVariable[KernelMessage](null)

  /**
   * Represents the current error stream used by the kernel for the specific
   * thread.
   */
  private val currentErrorStream =
    new DynamicVariable[PrintStream](null)
  private val currentErrorKernelMessage =
    new DynamicVariable[KernelMessage](null)

  /**
   * Represents magics available through the kernel.
   */
  val magics = new MagicManager(pluginManager)

  /**
   * Represents magic parsing functionality.
   */
  val magicParser = new MagicParser(magics)

  /**
   * Represents the data that can be shared using the kernel as the middleman.
   *
   * @note Using Java structure to enable other languages to have easy access!
   */
  val data: java.util.Map[String, Any] = new ConcurrentHashMap[String, Any]()

  val interpreter = interpreterManager.defaultInterpreter.get

  /**
   * Handles the output of interpreting code.
   *
   * @param output the output of the interpreter
   * @return (success, message) or (failure, message)
   */
  private def handleInterpreterOutput(
    output: (Result, Either[ExecuteOutput, ExecuteFailure])
  ): (Boolean, ExecuteOutput) = {
    val (success, result) = output
    success match {
      case Results.Success =>
        (true, result.left.get)
      case Results.Error =>
        (false, Map("text/plain" -> result.right.getOrElse("").toString))
      case Results.Aborted =>
        (false, Map("text/plain" -> "Aborted!"))
      case Results.Incomplete =>
        // If we get an incomplete it's most likely a syntax error, so
        // let the user know.
        (false, Map("text/plain" -> "Syntax Error!"))
    }
  }

  override def config:Config = {
    _config
  }

  /**
   * Executes a block of code represented as a string and returns the result.
   *
   * @param code The code as an option to execute
   * @return A tuple containing the result (true/false) and the output as a
   *         string
   */
  def eval(code: Option[String]): (Boolean, ExecuteOutput) = {
    code.map(c => {
      magicParser.parse(c) match {
        case Left(parsedCode) =>
          val output = interpreter.interpret(parsedCode)
          handleInterpreterOutput(output)
        case Right(errMsg) =>
          (false, Map("text/plain" -> errMsg))
      }
    }).getOrElse((false, Map("text/plain" -> "Error!")))
  }

  /**
   * Constructs a new instance of the stream methods using the latest
   * kernel message instance.
   *
   * @return The collection of stream methods
   */
  override def stream: StreamMethods = stream()

  /**
   * Constructs a new instance of the stream methods using the specified
   * kernel message instance.
   *
   * @param parentMessage The message to serve as the parent of outgoing
   *                      messages sent as a result of using streaming methods
   * @return The collection of streaming methods
   */
  private[toree] def stream(
    parentMessage: v5.KernelMessage = lastKernelMessage()
  ): StreamMethods = {
    new StreamMethods(actorLoader, parentMessage)
  }

  /**
   * Returns a collection of methods that can be used to display data from the
   * kernel to the client.
   *
   * @return The collection of display methods
   */
  override def display: DisplayMethodsLike = display()

  /**
   * Constructs a new instance of the stream methods using the specified
   * kernel message instance.
   *
   * @param parentMessage The message to serve as the parent of outgoing
   *                      messages sent as a result of using streaming methods
   * @return The collection of streaming methods
   */
  private[toree] def display(
    parentMessage: v5.KernelMessage = lastKernelMessage(),
    kmBuilder: v5.KMBuilder = v5.KMBuilder()
  ): DisplayMethods = {
    new DisplayMethods(actorLoader, parentMessage, kmBuilder)
  }

  /**
   * Constructs a new instance of the factory methods using the latest
   * kernel message instance.
   *
   * @return The collection of factory methods
   */
  override def factory: FactoryMethods = factory()

  /**
   * Constructs a new instance of the factory methods using the specified
   * kernel message and kernel message builder.
   *
   * @param parentMessage The message to serve as the parent of outgoing
   *                      messages sent as a result of using an object created
   *                      by the factory methods
   * @param kmBuilder The builder to be used by objects created by factory
   *                  methods
   * @return The collection of factory methods
   */
  private[toree] def factory(
    parentMessage: v5.KernelMessage = lastKernelMessage(),
    kmBuilder: v5.KMBuilder = v5.KMBuilder()
  ): FactoryMethods = {
    new FactoryMethods(_config, actorLoader, parentMessage, kmBuilder)
  }

  /**
   * Returns a print stream to be used for communication back to clients
   * via standard out.
   *
   * @return The print stream instance or an error if the stream info is
   *         not found
   */
  override def out: PrintStream = {
    val kernelMessage = lastKernelMessage()

    constructStream(currentOutputStream, currentOutputKernelMessage, kernelMessage, { kernelMessage =>
      val outputStream = this.factory(parentMessage = kernelMessage)
        .newKernelOutputStream("stdout")

      new PrintStream(outputStream)
    })
  }

  /**
   * Returns a print stream to be used for communication back to clients
   * via standard error.
   *
   * @return The print stream instance or an error if the stream info is
   *         not found
   */
  override def err: PrintStream = {
    val kernelMessage = lastKernelMessage()

    constructStream(currentErrorStream, currentErrorKernelMessage, kernelMessage, { kernelMessage =>
      val outputStream = this.factory(parentMessage = kernelMessage)
        .newKernelOutputStream("stderr")

      new PrintStream(outputStream)
    })
  }

  /**
   * Returns an input stream to be used to receive information from the client.
   *
   * @return The input stream instance or an error if the stream info is
   *         not found
   */
  override def in: InputStream = {
    val kernelMessage = lastKernelMessage()

    constructStream(currentInputStream, currentInputKernelMessage, kernelMessage, { kernelMessage =>
      this.factory(parentMessage = kernelMessage).newKernelInputStream()
    })
  }

  /**
   * Constructs or uses an existing stream.
   *
   * @param dynamicStream The DynamicVariable containing the stream to modify
   *                      or use
   * @param dynamicKernelMessage The DynamicVariable containing the KernelMessage to
   *                          check against the new KernelMessage
   * @param newKernelMessage The potentially-new KernelMessage
   * @param streamConstructionFunc The function used to create a new stream
   * @param typeTag The type information associated with the stream
   * @tparam T The stream type
   * @return The new stream or existing stream
   */
  private def constructStream[T](
    dynamicStream: DynamicVariable[T],
    dynamicKernelMessage: DynamicVariable[KernelMessage],
    newKernelMessage: KernelMessage,
    streamConstructionFunc: (KernelMessage) => T
  )(implicit typeTag: TypeTag[T]) = {
    // Update the stream being used only if the information has changed
    // or if the stream has not been initialized
    if (updateKernelMessage(dynamicKernelMessage, newKernelMessage) ||
      dynamicStream.value == null)
    {
      logger.trace("Creating new kernel " + typeTag.tpe.toString + "!")
      dynamicStream.value = streamConstructionFunc(newKernelMessage)
    }

    dynamicStream.value
  }

  /**
   * Updates the last stream info returning the status of whether or not the
   * new stream info was different than the last stream info.
   *
   * @param dynamicKernelMessage The dynamic variable containing the current
   *                          stream info
   * @param kernelMessage The new stream info
   * @return True if the new stream info is different from the last (therefore
   *         replaced), otherwise false
   */
  private def updateKernelMessage(
    dynamicKernelMessage: DynamicVariable[KernelMessage],
    kernelMessage: KernelMessage
  ): Boolean =
    if (kernelMessage != null && !kernelMessage.equals(dynamicKernelMessage.value)) {
      dynamicKernelMessage.value = kernelMessage
      true
    } else {
      false
    }

  /**
   * Retrieves the last kernel message received by the kernel.
   *
   * @throws IllegalArgumentException If no kernel message has been received
   * @return The kernel message instance
   */
  private def lastKernelMessage() = {
    val someKernelMessage = ExecuteRequestState.lastKernelMessage
    require(someKernelMessage.nonEmpty, "No kernel message received!")
    someKernelMessage.get
  }

  // TODO: Think of a better way to test without exposing this
  protected[toree] def createSparkConf(conf: SparkConf) = {

    if(conf.contains("spark.submit.deployMode")) {
      logger.info("Utilizing deploy mode: " + conf.get("spark.submit.deployMode"))
    } else {
      logger.info("Setting deployMode to client")
      conf.set("spark.submit.deployMode", "client")
    }

    conf
  }

  // TODO: Think of a better way to test without exposing this
  protected[toree] def initializeSparkContext(sparkConf: SparkConf): SparkContext = {

    logger.debug("Constructing new Spark Context")
    // TODO: Inject stream redirect headers in Spark dynamically
    var sparkContext: SparkContext = null
    val outStream = new KernelOutputStream(
      actorLoader, KMBuilder(), global.ScheduledTaskManager.instance,
      sendEmptyOutput = _config.getBoolean("send_empty_output")
    )

    // Update global stream state and use it to set the Console local variables
    // for threads in the Spark threadpool
    global.StreamState.setStreams(System.in, outStream, outStream)
    global.StreamState.withStreams {
      sparkContext = new SparkContext(sparkConf)
    }

    sparkContext
  }

  // TODO: Exposed for testing purposes.
  protected[toree] def getSparkContextInitializationTimeout: Long = {
    val timeout:Long = config.getDuration("spark_context_initialization_timeout", TimeUnit.MILLISECONDS)
    if (timeout <= 0) {
      val clOptionName = "spark_context_initialization_timeout"
      throw new RuntimeException(s"--$clOptionName: Invalid timeout of '$timeout' milliseconds specified. " +
        s"Must specify a positive value.")
    }
    timeout
  }

  override def interpreter(name: String): Option[Interpreter] = {
    interpreterManager.interpreters.get(name)
  }

  private lazy val defaultSparkConf: SparkConf = createSparkConf(new SparkConf())

  override def sparkSession: SparkSession = {

    if(config.getString("spark_context_initialization_mode") == "eager") {
      // explicitly enable eager initialization of spark context
      SparkSession.builder.config(defaultSparkConf).getOrCreate
    } else {
      // default lazy initialization of spark context
      defaultSparkConf.getOption("spark.master") match {
        case Some(master) if !master.contains("local") =>
          // When connecting to a remote cluster, the first call to getOrCreate
          // may create a session and take a long time, so this starts a future
          // to get the session. If it take longer than specified timeout, then
          // print a message to the user that Spark is starting. Note, the
          // default timeout is 100ms and it is specified in reference.conf.
          import scala.concurrent.ExecutionContext.Implicits.global
          val sessionFuture = Future {
            SparkSession.builder.config(defaultSparkConf).getOrCreate
          }

          try {
            val timeout = getSparkContextInitializationTimeout
            Await.result(sessionFuture, Duration(timeout, TimeUnit.MILLISECONDS))
          } catch {
            case timeout: TimeoutException =>
              // getting the session is taking a long time, so assume that Spark
              // is starting and print a message
              display.content(
                MIMEType.PlainText, "Waiting for a Spark session to start...")
              Await.result(sessionFuture, Duration.Inf)
          }

        case _ =>
          SparkSession.builder.config(defaultSparkConf).getOrCreate
      }
    }
  }

  override def sparkContext: SparkContext = sparkSession.sparkContext
  override def sparkConf: SparkConf = sparkSession.sparkContext.getConf
  override def javaSparkContext: JavaSparkContext = javaSparkContext(sparkSession)

  private val javaContexts = new mutable.WeakHashMap[SparkSession, JavaSparkContext]
  private def javaSparkContext(sparkSession: SparkSession): JavaSparkContext = {
    javaContexts.synchronized {
      javaContexts.getOrElseUpdate(
        sparkSession,
        new JavaSparkContext(sparkSession.sparkContext))
    }
  }
}
