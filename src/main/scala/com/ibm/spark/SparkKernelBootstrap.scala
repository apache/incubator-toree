package com.ibm.spark

import akka.actor._
import com.ibm.spark.interpreter.{ScalaInterpreter, Interpreter}
import com.ibm.spark.kernel.protocol.v5.MessageType.MessageType
import com.ibm.spark.kernel.protocol.v5.SocketType.SocketType
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.handler._
import com.ibm.spark.kernel.protocol.v5.interpreter.InterpreterActor
import com.ibm.spark.kernel.protocol.v5.interpreter.tasks.InterpreterTaskFactory
import com.ibm.spark.kernel.protocol.v5.socket._
import org.apache.spark.{SparkContext, SparkConf}
import org.slf4j.LoggerFactory

case class SparkKernelBootstrap(sparkKernelOptions: SparkKernelOptions) {
  private val logger = LoggerFactory.getLogger(classOf[SparkKernelBootstrap])

  private val DefaultSparkMaster                      = sparkKernelOptions.master.getOrElse("local[*]")
  private val DefaultAppName                          = SparkKernelInfo.banner
  private val DefaultActorSystemName                  = "spark-kernel-actor-system"

  private var socketConfigReader: SocketConfigReader  = _
  private var socketFactory: SocketFactory            = _
  private var heartbeatActor: ActorRef                = _
  private var shellActor: ActorRef                    = _
  private var ioPubActor: ActorRef                    = _

  private var interpreter: Interpreter                = _
  private var sparkContext: SparkContext              = _

  private var actorSystem: ActorSystem                = _
  private var actorLoader: ActorLoader                = _
  private var interpreterActor: ActorRef              = _
  private var relayActor: ActorRef                    = _

  /**
   * Initializes all kernel systems.
   */
  def initialize() = {
    initializeInterpreter()
    initializeSparkContext()
    initializeSystemActors()
    initializeKernelHandlers()
    createSockets()
    registerShutdownHook()

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

  private def createSockets(): Unit = {
    logger.info("Creating sockets")

    logger.debug("Constructing SocketConfigReader")
    socketConfigReader = new SocketConfigReader(sparkKernelOptions.profile)

    logger.debug("Constructing SocketFactory")
    socketFactory = new SocketFactory(socketConfigReader.getSocketConfig)

    logger.debug("Initializing Heartbeat on port " +
      socketConfigReader.getSocketConfig.hb_port)
    heartbeatActor = actorSystem.actorOf(
      Props(classOf[Heartbeat], socketFactory),
      name = SocketType.Heartbeat.toString
    )

    logger.debug("Initializing Shell on port " +
      socketConfigReader.getSocketConfig.shell_port)
    shellActor = actorSystem.actorOf(
      Props(classOf[Shell], socketFactory, actorLoader),
      name = SocketType.Shell.toString
    )

    logger.debug("Initializing IOPub on port " +
      socketConfigReader.getSocketConfig.iopub_port)
    ioPubActor = actorSystem.actorOf(
      Props(classOf[IOPub], socketFactory),
      name = SocketType.IOPub.toString
    )
  }

  private def initializeInterpreter(): Unit = {
    val tailOptions = sparkKernelOptions.tail

    logger.info("Constructing interpreter with " + tailOptions.mkString(" "))
    interpreter = new ScalaInterpreter(tailOptions, Console.out)

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

    val master = DefaultSparkMaster
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
    val sparkIMain = interpreter.asInstanceOf[ScalaInterpreter].sparkIMain

    sparkIMain.beQuietDuring {
      // TODO: Construct class server outside of SparkIMain
      logger.warn("Unable to control initialization of REPL class server!")
      logger.info("REPL Class Server Uri: " + sparkIMain.classServer.uri)
      conf.set("spark.repl.class.uri", sparkIMain.classServer.uri)

      logger.info("Constructing new Spark Context")
      sparkContext = new SparkContext(conf)
      sparkIMain.bind(
        "sc", "org.apache.spark.SparkContext",
        sparkContext, List( """@transient"""))
    }
  }

  private def initializeSystemActors(): Unit = {
    logger.info("Initializing internal actor system")
    actorSystem = ActorSystem(DefaultActorSystemName)

    logger.info("Creating Simple Actor Loader")
    actorLoader = SimpleActorLoader(actorSystem)

    logger.info("Creating interpreter actor")
    interpreterActor = actorSystem.actorOf(
      Props(classOf[InterpreterActor], new InterpreterTaskFactory(interpreter)),
      name = SystemActorType.Interpreter.toString
    )

    logger.info("Creating relay actor")
    relayActor = actorSystem.actorOf(
      Props(classOf[Relay], actorLoader),
      name = SystemActorType.Relay.toString
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
    initializeSocketHandler(SocketType.IOPub, MessageType.ExecuteInput)
    initializeSocketHandler(SocketType.IOPub, MessageType.Status)
  }

}

