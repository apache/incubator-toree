/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.spark

import akka.actor.{Props, ActorSystem, ActorRef}
import com.ibm.spark.dependencies.{IvyDependencyDownloader, DependencyDownloader}
import com.ibm.spark.interpreter._
import com.ibm.spark.kernel.protocol.v5.KernelStatusType._
import com.ibm.spark.kernel.protocol.v5.MessageType.MessageType
import com.ibm.spark.kernel.protocol.v5.SocketType.SocketType
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.dispatch.StatusDispatch
import com.ibm.spark.kernel.protocol.v5.handler._
import com.ibm.spark.kernel.protocol.v5.interpreter.InterpreterActor
import com.ibm.spark.kernel.protocol.v5.interpreter.tasks.InterpreterTaskFactory
import com.ibm.spark.kernel.protocol.v5.magic.MagicManager
import com.ibm.spark.kernel.protocol.v5.relay.{ExecuteRequestRelay, KernelMessageRelay}
import com.ibm.spark.kernel.protocol.v5.security.SignatureManagerActor
import com.ibm.spark.kernel.protocol.v5.socket._
import com.ibm.spark.kernel.protocol.v5.stream.KernelMessageStream
import com.ibm.spark.magic.MagicLoader
import com.ibm.spark.magic.builtin.BuiltinLoader
import com.ibm.spark.magic.dependencies.DependencyMap
import com.ibm.spark.security.KernelSecurityManager
import com.ibm.spark.utils.{GlobalStreamState, LogLike}
import com.typesafe.config.Config
import org.apache.spark.{SparkConf, SparkContext}
import collection.JavaConverters._
import play.api.libs.json.Json
import java.io.File

case class SparkKernelBootstrap(config: Config) extends LogLike {

  private val DefaultAppName                          = SparkKernelInfo.banner
  private val DefaultActorSystemName                  = "spark-kernel-actor-system"

  private var socketFactory: ServerSocketFactory            = _
  private var heartbeatActor: ActorRef                = _
  private var shellActor: ActorRef                    = _
  private var ioPubActor: ActorRef                    = _

  protected[spark] var interpreter: Interpreter       = _
  protected[spark] var sparkContext: SparkContext     = _
  protected[spark] var dependencyDownloader: DependencyDownloader = _
  private var dependencyMap: DependencyMap            = _
  private var builtinLoader: BuiltinLoader            = _
  private var magicLoader: MagicLoader                = _

  private var actorSystem: ActorSystem                = _
  private var actorLoader: ActorLoader                = _
  private var interpreterActor: ActorRef              = _
  private var kernelMessageRelayActor: ActorRef       = _
  private var executeRequestRelayActor: ActorRef      = _
  private var signatureManagerActor: ActorRef         = _
  private var magicManagerActor: ActorRef             = _
  private var statusDispatch: ActorRef                = _

  /**
   * Initializes all kernel systems.
   */
  def initialize() = {
    // TODO: Investigate potential to initialize System out/err/in to capture
    //       Console DynamicVariable initialization (since takes System fields)
    //       and redirect it to a workable location (like an actor) with the
    //       thread's current information attached
    //
    // E.G. System.setOut(customPrintStream) ... all new threads will have
    //      customPrintStream as their initial Console.out value
    //

    initializeBareComponents()
    createSockets()
    initializeKernelHandlers()
    publishStatus(KernelStatusType.Starting)
    initializeInterpreter()
    initializeSparkContext()
    initializeDependencyDownloader()
    initializeMagicLoader()
    initializeSystemActors()
    registerInterruptHook()
    registerShutdownHook()

    logger.debug("Initializing security manager")
    System.setSecurityManager(new KernelSecurityManager)

    logger.info("Marking relay as ready for receiving messages")
    kernelMessageRelayActor ! true

    this
  }

  /**
   * Shuts down all kernel systems.
   */
  def shutdown() = {
    logger.info("Shutting down Spark Context")
    sparkContext.stop()

    logger.info("Shutting down interpreter")
    interpreter.stop()

    logger.info("Shutting down actor system")
    actorSystem.shutdown()

    this
  }

  /**
   * Waits for the main actor system to terminate.
   */
  def waitForTermination() = {
    logger.debug("Waiting for actor system to terminate")
    actorSystem.awaitTermination()

    this
  }

  /**
   * Does minimal setup in order to send the "starting" status message over
   * the IOPub socket
   */
  private def initializeBareComponents(): Unit = {
    logger.info("Initializing internal actor system")
    actorSystem = ActorSystem(DefaultActorSystemName)

    logger.debug("Creating Simple Actor Loader")
    actorLoader = SimpleActorLoader(actorSystem)

    logger.debug("Creating kernel message relay actor")
    kernelMessageRelayActor = actorSystem.actorOf(
      Props(
        classOf[KernelMessageRelay], actorLoader, true
      ),
      name = SystemActorType.KernelMessageRelay.toString
    )

    logger.debug("Creating signature manager actor")
    val sigKey = config.getString("key")
    val sigScheme = config.getString("signature_scheme")
    logger.debug("Key = " + sigKey)
    logger.debug("Scheme = " + sigScheme)
    signatureManagerActor = actorSystem.actorOf(
      Props(
        classOf[SignatureManagerActor], sigKey, sigScheme.replace("-", "")
      ),
      name = SystemActorType.SignatureManager.toString
    )

    logger.debug("Creating status dispatch actor")
    statusDispatch = actorSystem.actorOf(
      Props(classOf[StatusDispatch], actorLoader),
      name = SystemActorType.StatusDispatch.toString
    )
  }

  private def createSockets(): Unit = {
    logger.debug("Creating sockets")

    val socketConfig: SocketConfig = SocketConfig.fromConfig(config)
    logger.info("Connection Profile: "
      + Json.prettyPrint(Json.toJson(socketConfig)))

    logger.debug("Constructing ServerSocketFactory")
    socketFactory = new ServerSocketFactory(socketConfig)

    logger.debug("Initializing Heartbeat on port " +
      socketConfig.hb_port)
    heartbeatActor = actorSystem.actorOf(
      Props(classOf[Heartbeat], socketFactory),
      name = SocketType.Heartbeat.toString
    )

    logger.debug("Initializing Shell on port " +
      socketConfig.shell_port)
    shellActor = actorSystem.actorOf(
      Props(classOf[Shell], socketFactory, actorLoader),
      name = SocketType.Shell.toString
    )

    logger.debug("Initializing IOPub on port " +
      socketConfig.iopub_port)
    ioPubActor = actorSystem.actorOf(
      Props(classOf[IOPub], socketFactory),
      name = SocketType.IOPub.toString
    )
  }

  private def initializeDependencyDownloader(): Unit = {
    dependencyDownloader = new IvyDependencyDownloader(
      "http://repo1.maven.org/maven2/", config.getString("ivy_local")
    )
  }

  private def initializeInterpreter(): Unit = {
    val interpreterArgs = config.getStringList("interpreter_args").asScala.toList

    logger.info("Constructing interpreter with arguments: " + interpreterArgs.mkString(" "))
    interpreter = new ScalaInterpreter(interpreterArgs, Console.out)
      with StandardSparkIMainProducer
      with StandardTaskManagerProducer
      with StandardSettingsProducer

    logger.debug("Starting interpreter")
    interpreter.start()
  }

  private def registerInterruptHook(): Unit = {
    val self = this

    import sun.misc.{Signal, SignalHandler}

    // TODO: Signals are not a good way to handle this since JVM only has the
    // proprietary sun API that is not necessarily available on all platforms
    Signal.handle(new Signal("INT"), new SignalHandler() {
      private val MaxSignalTime: Long = 3000 // 3 seconds
      var lastSignalReceived: Long    = 0

      def handle(sig: Signal) = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSignalReceived > MaxSignalTime) {
          logger.info("Resetting code execution!")
          interpreter.interrupt()

          // TODO: Cancel group representing current code execution
          //sparkContext.cancelJobGroup()

          logger.info("Enter Ctrl-C twice to shutdown!")
          lastSignalReceived = currentTime
        } else {
          logger.info("Shutting down kernel")
          self.shutdown()
        }
      }
    })
  }

  private def registerShutdownHook(): Unit = {
    logger.debug("Registering shutdown hook")
    val self = this
    val mainThread = Thread.currentThread()
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() = {
        logger.info("Shutting down kernel")
        self.shutdown()
        // TODO: Check if you can magically access the spark context to stop it
        // TODO: inside a different thread
        if (mainThread.isAlive) mainThread.join()
      }
    })
  }

  protected[spark] def initializeSparkContext(): Unit = {
    logger.debug("Creating Spark Configuration")
    val conf = new SparkConf()

    val master = config.getString("spark.master")
    logger.info("Using " + master + " as Spark Master")
    conf.setMaster(master)

    val appName = DefaultAppName
    logger.info("Using " + appName + " as Spark application name")
    conf.setAppName(appName)

    // TODO: Add support for spark.executor.uri from environment variable or CLI
    logger.warn("spark.executor.uri is not supported!")
    //conf.set("spark.executor.uri", "...")

    // TODO: Add support for Spark Home from environment variable or CLI
    logger.warn("Spark Home is not supported!")
    //conf.setSparkHome("...")

    // TODO: Move SparkIMain to private and insert in a different way
    logger.warn("Locked to Scala interpreter with SparkIMain until decoupled!")

    reallyInitializeSparkContext(conf)
  }

  protected[spark] def reallyInitializeSparkContext(conf: SparkConf): Unit = {
    interpreter.doQuietly {
      // TODO: Construct class server outside of SparkIMain
      logger.warn("Unable to control initialization of REPL class server!")
      logger.info("REPL Class Server Uri: " + interpreter.classServerURI)
      conf.set("spark.repl.class.uri", interpreter.classServerURI)

      logger.debug("Constructing new Spark Context")
      // TODO: Inject stream redirect headers in Spark dynamically
      val outStream = new KernelMessageStream(actorLoader, KMBuilder())
      GlobalStreamState.withStreams(System.in, outStream, outStream) {
        sparkContext = new SparkContext(conf)
      }

      logger.debug("Binding context into interpreter")
      interpreter.bind(
        "sc", "org.apache.spark.SparkContext",
        sparkContext, List( """@transient"""))

      // NOTE: This is needed because interpreter blows up after adding
      //       dependencies to SparkContext and Interpreter before the
      //       cluster has been used... not exactly sure why this is the case
      // TODO: Investigate why the cluster has to be initialized in the kernel
      //       to avoid the kernel's interpreter blowing up (must be done
      //       inside the interpreter)
      logger.debug("Initializing Spark cluster in interpreter")
      interpreter.doQuietly {
        interpreter.interpret("""
          var $toBeNulled = sc.emptyRDD.collect()
          $toBeNulled = null
        """)
      }
    }

    // Add ourselves as a dependency
    // TODO: Provide ability to point to library as commandline argument
    // TODO: Provide better method to determine if can add ourselves
    // TODO: Avoid duplicating request for master twice (initializeSparkContext
    //       also does this)
    val master = config.getString("spark.master")
    // If in local mode, do not need to add our jar as a dependency
    if (!master.toLowerCase.startsWith("local")) {
      logger.info("Adding self as dependency from " +
        com.ibm.spark.SparkKernel.getClass.getProtectionDomain
          .getCodeSource.getLocation.getPath
      )
      // Assuming inside a jar if not in local mode
      sparkContext.addJar(
        com.ibm.spark.SparkKernel.getClass.getProtectionDomain
          .getCodeSource.getLocation.getPath
      )
    } else {
      logger.info("Running in local mode! Not adding self as dependency!")
    }
  }

  private def initializeSystemActors(): Unit = {
    logger.debug("Creating interpreter actor")
    interpreterActor = actorSystem.actorOf(
      Props(classOf[InterpreterActor], new InterpreterTaskFactory(interpreter)),
      name = SystemActorType.Interpreter.toString
    )


    logger.debug("Creating execute request relay actor")
    executeRequestRelayActor = actorSystem.actorOf(
      Props(classOf[ExecuteRequestRelay], actorLoader),
      name = SystemActorType.ExecuteRequestRelay.toString
    )


    logger.info("Creating magic manager actor")
    magicManagerActor = actorSystem.actorOf(
      Props(classOf[MagicManager], magicLoader),
      name = SystemActorType.MagicManager.toString
    )
  }

  private def initializeMagicLoader(): Unit = {
    logger.debug("Constructing magic loader")

    logger.debug("Building dependency map")
    dependencyMap = new DependencyMap()
      .setInterpreter(interpreter)
      .setSparkContext(sparkContext)
      .setDependencyDownloader(dependencyDownloader)

    logger.debug("Creating BuiltinLoader")
    builtinLoader = new BuiltinLoader()

    val magicUrlArray = config.getStringList("magic_urls").asScala
      .map(s => new File(s).toURI.toURL).toArray

    if (magicUrlArray.isEmpty)
      logger.warn("No external magics provided to MagicLoader!")
    else
      logger.info("Using magics from the following locations: " +
        magicUrlArray.map(_.getPath).mkString(","))

    logger.debug("Creating MagicLoader")
    magicLoader = new MagicLoader(
      dependencyMap = dependencyMap,
      urls = magicUrlArray,
      parentLoader = builtinLoader
    )
  }

  private def initializeRequestHandler[T](clazz: Class[T], messageType: MessageType){
    logger.debug("Creating %s handler".format(messageType.toString))
    actorSystem.actorOf(
      Props(clazz, actorLoader),
      name = messageType.toString
    )
  }

  private def initializeSocketHandler(socketType: SocketType, messageType: MessageType): Unit = {
    logger.debug("Creating %s to %s socket handler ".format(messageType.toString ,socketType.toString))
    actorSystem.actorOf(
      Props(classOf[GenericSocketMessageHandler], actorLoader, socketType),
      name = messageType.toString
    )
  }

  private def initializeKernelHandlers(): Unit = {
    //  These are the handlers for messages coming into the
    initializeRequestHandler(classOf[ExecuteRequestHandler], MessageType.ExecuteRequest )
    initializeRequestHandler(classOf[KernelInfoRequestHandler], MessageType.KernelInfoRequest )
    initializeRequestHandler(classOf[CodeCompleteHandler], MessageType.CompleteRequest )

    //  These are handlers for messages leaving the kernel through the sockets
    initializeSocketHandler(SocketType.Shell, MessageType.KernelInfoReply)
    initializeSocketHandler(SocketType.Shell, MessageType.ExecuteReply)
    initializeSocketHandler(SocketType.Shell, MessageType.CompleteReply)
    initializeSocketHandler(SocketType.IOPub, MessageType.ExecuteResult)
    initializeSocketHandler(SocketType.IOPub, MessageType.Stream)
    initializeSocketHandler(SocketType.IOPub, MessageType.ExecuteInput)
    initializeSocketHandler(SocketType.IOPub, MessageType.Status)
    initializeSocketHandler(SocketType.IOPub, MessageType.Error)
  }

  private def publishStatus(
    status: KernelStatusType,
    parentHeader: Option[ParentHeader] = None
  ): Unit = {
    parentHeader match {
      case Some(header) => statusDispatch ! ((status, header))
      case None         => statusDispatch ! status
    }
  }
}

