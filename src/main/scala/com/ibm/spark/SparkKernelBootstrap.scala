package com.ibm.spark

import akka.actor._
import com.ibm.spark.interpreter.{Interpreter, ScalaInterpreter}
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
import com.ibm.spark.magic.MagicLoader
import com.ibm.spark.magic.builtin.BuiltinLoader
import com.ibm.spark.magic.dependencies.DependencyMap
import com.ibm.spark.utils.LogLike
import com.typesafe.config.Config
import org.apache.spark.{SparkConf, SparkContext}
import play.api.libs.json.Json
import scala.collection.JavaConverters._

case class SparkKernelBootstrap(config: Config) extends LogLike {

  private val DefaultAppName                          = SparkKernelInfo.banner
  private val DefaultActorSystemName                  = "spark-kernel-actor-system"

  private var socketFactory: SocketFactory            = _
  private var heartbeatActor: ActorRef                = _
  private var shellActor: ActorRef                    = _
  private var ioPubActor: ActorRef                    = _

  private var interpreter: Interpreter                = _
  private var sparkContext: SparkContext              = _
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
    setup()
    createSockets()
    initializeKernelHandlers()
    publishStatus(KernelStatusType.Starting)
    initializeInterpreter()
    initializeSparkContext()
    initializeMagicLoader()
    initializeSystemActors()
    registerShutdownHook()
    publishStatus(KernelStatusType.Idle)

    this
  }

  /**
   * Shuts down all kernel systems.
   */
  def shutdown() = {
    logger.info("Shutting down Spark Context")
    sparkContext.stop()

    logger.info("Shutting down interpreter")
    interpreter.stop

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
  private def setup(): Unit = {
    logger.info("Initializing internal actor system")
    actorSystem = ActorSystem(DefaultActorSystemName)

    logger.info("Creating Simple Actor Loader")
    actorLoader = SimpleActorLoader(actorSystem)

    logger.info("Creating kernel message relay actor")
    kernelMessageRelayActor = actorSystem.actorOf(
      Props(classOf[KernelMessageRelay], actorLoader),
      name = SystemActorType.KernelMessageRelay.toString
    )

    logger.info("Creating signature manager actor")
    val sigKey = config.getString("key")
    val sigScheme = config.getString("signature_scheme")
    logger.info("Key = " + sigKey)
    logger.info("Scheme = " + sigScheme)
    signatureManagerActor = actorSystem.actorOf(
      Props(classOf[SignatureManagerActor], sigKey, sigScheme.replace("-", "")),
      name = SystemActorType.SignatureManager.toString
    )

    logger.info("Creating status dispatch actor")
    statusDispatch = actorSystem.actorOf(
      Props(classOf[StatusDispatch], actorLoader),
      name = SystemActorType.StatusDispatch.toString
    )
  }

  private def createSockets(): Unit = {
    logger.info("Creating sockets")

    val socketConfig: SocketConfig = SocketConfig.fromConfig(config)
    logger.info("Connection Profile: " + Json.toJson(socketConfig))

    logger.debug("Constructing SocketFactory")
    socketFactory = new SocketFactory(socketConfig)

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

  private def initializeInterpreter(): Unit = {
    val interpreterArgs = config.getStringList("interpreter_args").asScala.toList

    logger.info("Constructing interpreter with arguments: " + interpreterArgs.mkString(" "))
    interpreter = new ScalaInterpreter(interpreterArgs, Console.out)

    logger.debug("Starting interpreter")
    interpreter.start
  }

  private def registerShutdownHook(): Unit = {
    logger.info("Registering shutdown hook")
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

  private def initializeSparkContext(): Unit = {
    logger.debug("Creating Spark Configuration")
    val conf = new SparkConf()

    val master = config.getString("master")
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


    interpreter.doQuietly {
      // TODO: Construct class server outside of SparkIMain
      logger.warn("Unable to control initialization of REPL class server!")
      logger.info("REPL Class Server Uri: " + interpreter.classServerURI)
      conf.set("spark.repl.class.uri", interpreter.classServerURI)

      logger.info("Constructing new Spark Context")
      sparkContext = new SparkContext(conf)
      interpreter.bind(
        "sc", "org.apache.spark.SparkContext",
        sparkContext, List( """@transient"""))

    }
  }

  private def initializeSystemActors(): Unit = {
    logger.info("Creating interpreter actor")
    interpreterActor = actorSystem.actorOf(
      Props(classOf[InterpreterActor], new InterpreterTaskFactory(interpreter)),
      name = SystemActorType.Interpreter.toString
    )


    logger.info("Creating execute request relay actor")
    executeRequestRelayActor = actorSystem.actorOf(
      Props(classOf[ExecuteRequestRelay], actorLoader),
      name = SystemActorType.ExecuteRequestRelay.toString
    )


    logger.info("Creating magic manager actor")
    logger.warn("MagicManager has a MagicLoader that is empty!")
    magicManagerActor = actorSystem.actorOf(
      Props(classOf[MagicManager], magicLoader),
      name = SystemActorType.MagicManager.toString
    )
  }

  private def initializeMagicLoader(): Unit = {
    logger.info("Constructing magic loader")

    logger.debug("Building dependency map")
    dependencyMap = new DependencyMap()
      .setInterpreter(interpreter)
      .setSparkContext(sparkContext)

    logger.debug("Creating BuiltinLoader")
    builtinLoader = new BuiltinLoader()

    logger.debug("Creating MagicLoader")
    magicLoader = new MagicLoader(
      dependencyMap = dependencyMap,
      parentLoader = builtinLoader
    )
  }

  private def initializeRequestHandler[T](clazz: Class[T], messageType: MessageType){
    logger.info("Creating %s handler".format(messageType.toString))
    actorSystem.actorOf(
      Props(clazz, actorLoader),
      name = messageType.toString
    )
  }

  private def initializeSocketHandler(socketType: SocketType, messageType: MessageType): Unit = {
    logger.info("Creating %s to %s socket handler ".format(messageType.toString ,socketType.toString))
    actorSystem.actorOf(
      Props(classOf[GenericSocketMessageHandler], actorLoader, socketType),
      name = messageType.toString
    )
  }

  private def initializeKernelHandlers(): Unit = {
    //  These are the handlers for messages coming into the
    initializeRequestHandler(classOf[ExecuteRequestHandler], MessageType.ExecuteRequest )
    initializeRequestHandler(classOf[KernelInfoRequestHandler], MessageType.KernelInfoRequest )

    //  These are handlers for messages leaving the kernel through the sockets
    initializeSocketHandler(SocketType.Shell, MessageType.KernelInfoReply)
    initializeSocketHandler(SocketType.Shell, MessageType.ExecuteReply)
    initializeSocketHandler(SocketType.IOPub, MessageType.ExecuteResult)
    initializeSocketHandler(SocketType.IOPub, MessageType.Stream)
    initializeSocketHandler(SocketType.IOPub, MessageType.ExecuteInput)
    initializeSocketHandler(SocketType.IOPub, MessageType.Status)
    initializeSocketHandler(SocketType.IOPub, MessageType.Error)
  }

  private def publishStatus(status: KernelStatusType): Unit = {
    statusDispatch ! status
  }
}

