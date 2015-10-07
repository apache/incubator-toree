/*
 * Copyright 2014 IBM Corp.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.ibm.spark.boot.layer

import java.io.OutputStream

import akka.actor.{Actor, Props, ActorRef, ActorSystem}
import akka.testkit.TestProbe
import com.ibm.spark.boot.{CommandLineOptions, KernelBootstrap}
import com.ibm.spark.kernel.interpreter.scala.{StandardTaskManagerProducer, StandardSparkIMainProducer, StandardSettingsProducer, ScalaInterpreter}
import com.ibm.spark.kernel.protocol.v5.{KMBuilder, SocketType}
import com.ibm.spark.kernel.protocol.v5.kernel.ActorLoader
import com.ibm.spark.kernel.protocol.v5.kernel.socket._
import com.ibm.spark.utils.LogLike
import com.typesafe.config.Config
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import scala.collection.JavaConverters._
import test.utils.SparkContextProvider
import org.apache.spark.{SparkContext, SparkConf}
import com.ibm.spark.kernel.protocol.v5.stream.KernelOutputStream
import com.ibm.spark.global

/**
 * Represents an object that can deploy a singleton Spark Kernel for tests,
 * providing access to the actors used for socket communication.
 */
object SparkKernelDeployer extends LogLike with MockitoSugar {
  private var actorSystem: ActorSystem = _
  private var actorLoader: ActorLoader = _
  private var heartbeatProbe: TestProbe = _
  private var heartbeatActor: ActorRef = _
  private var stdinProbe: TestProbe = _
  private var stdinActor: ActorRef = _
  private var shellProbe: TestProbe = _
  private var shellActor: ActorRef = _
  private var ioPubProbe: TestProbe = _
  private var ioPubActor: ActorRef = _

  private class ActorInterceptor(testProbe: TestProbe, actor: ActorRef)
    extends Actor
  {
    override def receive: Receive = {
      case m =>
        testProbe.ref.forward(m)
        actor ! m
    }
  }

  private trait ExposedComponentInitialization extends StandardComponentInitialization
    with LogLike {
    override protected def initializeInterpreter(config: Config): ScalaInterpreter
      with StandardSparkIMainProducer with StandardTaskManagerProducer
      with StandardSettingsProducer = {
      val interpreterArgs = config.getStringList("interpreter_args").asScala.toList

      logger.info("Constructing interpreter with arguments: " +
        interpreterArgs.mkString(" "))
      val interpreter = new ScalaInterpreter(interpreterArgs, mock[OutputStream])
        with StandardSparkIMainProducer
        with StandardTaskManagerProducer
        with StandardSettingsProducer

      logger.debug("Starting interpreter")
      interpreter.start()

      interpreter
    }

    override protected[layer] def reallyInitializeSparkContext(
      config: Config,
      actorLoader: ActorLoader,
      kmBuilder: KMBuilder,
      sparkConf: SparkConf
    ): SparkContext = {
      logger.debug("Constructing new Spark Context")
      // TODO: Inject stream redirect headers in Spark dynamically
      var sparkContext: SparkContext = null
      val outStream = new KernelOutputStream(
        actorLoader, KMBuilder(), global.ScheduledTaskManager.instance)
      global.StreamState.setStreams(System.in, outStream, outStream)
      global.StreamState.withStreams {
        sparkContext = SparkContextProvider.sparkContext
      }

      sparkContext
     }

  }

  /**
   * Runs bare initialization, wrapping socket actors with test logic to
   * intercept messages.
   */
  private trait ExposedBareInitialization
    extends StandardBareInitialization with LogLike
  {
    override protected def createSockets(
      config: Config, actorSystem: ActorSystem, actorLoader: ActorLoader
    ): (ActorRef, ActorRef, ActorRef, ActorRef) =
    {
      logger.debug("Creating sockets")

      val socketConfig: SocketConfig = SocketConfig.fromConfig(config)
      logger.info("Connection Profile: "
        + Json.prettyPrint(Json.toJson(socketConfig)))

      logger.debug("Constructing ServerSocketFactory")
      val socketFactory = new SocketFactory(socketConfig)

      logger.debug("Initializing Heartbeat on port " +
        socketConfig.hb_port)
      val testHeartbeatActor = actorSystem.actorOf(
        Props(classOf[Heartbeat], socketFactory)
      )

      logger.debug("Initializing Stdin on port " +
        socketConfig.stdin_port)
      val testStdinActor = actorSystem.actorOf(
        Props(classOf[Stdin], socketFactory, actorLoader)
      )

      logger.debug("Initializing Shell on port " +
        socketConfig.shell_port)
      val testShellActor = actorSystem.actorOf(
        Props(classOf[Shell], socketFactory, actorLoader)
      )

      logger.debug("Initializing IOPub on port " +
        socketConfig.iopub_port)
      val testIOPubActor = actorSystem.actorOf(
        Props(classOf[IOPub], socketFactory)
      )

      // Update our publicly-available probes and create actors to wrap them
      heartbeatProbe = new TestProbe(actorSystem)
      val heartbeatInterceptor = actorSystem.actorOf(
        Props(new ActorInterceptor(heartbeatProbe, testHeartbeatActor)),
        name = SocketType.Heartbeat.toString
      )
      stdinProbe = new TestProbe(actorSystem)
      val stdinInterceptor = actorSystem.actorOf(
        Props(new ActorInterceptor(stdinProbe, testStdinActor)),
        name = SocketType.StdIn.toString
      )
      shellProbe = new TestProbe(actorSystem)
      val shellInterceptor = actorSystem.actorOf(
        Props(new ActorInterceptor(shellProbe, testShellActor)),
        name = SocketType.Shell.toString
      )
      ioPubProbe = new TestProbe(actorSystem)
      val ioPubInterceptor = actorSystem.actorOf(
        Props(new ActorInterceptor(ioPubProbe, testIOPubActor)),
        name = SocketType.IOPub.toString
      )

      (heartbeatInterceptor, stdinInterceptor, shellInterceptor,
        ioPubInterceptor)
    }

    override protected def createActorLoader(actorSystem: ActorSystem) = {
      // Grab the created actor loader so we can use it with TestKit
      actorLoader = super.createActorLoader(actorSystem)
      actorLoader
    }

    override protected def createActorSystem(actorSystemName: String) = {
      // Grab the created actor system so we can use it with TestKit
      require(actorSystem != null,
        "Actor System not initialized before bootstrap!")
      //actorSystem = super.createActorSystem(actorSystemName)
      actorSystem
    }
  }

  /**
   * Represents the singleton KernelBootstrap instance that does not
   * receive any external commandline arguments.
   */
  lazy val noArgKernelBootstrap = {
    // Print out a message to indicate this fixture is being created
    logger.debug("Creating 'no external args' Spark Kernel through Kernel Bootstrap")

    val kernelBootstrap =
      (new KernelBootstrap(new CommandLineOptions(Nil).toConfig)
        with ExposedBareInitialization
        with ExposedComponentInitialization
        with StandardHandlerInitialization
        with StandardHookInitialization).initialize()

    logger.debug("Finished initializing Kernel Bootstrap! Testing can now start!")

    kernelBootstrap
  }

  /**
   * Provides a gateway for tests to receive the "no external arguments"
   * KernelBootstrap through an actor loader and socket test probes.
   *
   * @param testCode The test code to execute
   *
   * @return The results from the test code
   */
  def withNoArgSparkKernel(
    testCode: (ActorLoader, TestProbe, TestProbe, TestProbe) => Any
  ) = testCode(actorLoader, heartbeatProbe, shellProbe, ioPubProbe)


  /**
   * Retrieves the actor system for the "no external arguments" KernelBootstrap
   * instance. Will initialize the KernelBootstrap if the actor system is not
   * ready.
   *
   * @return The actor system of KernelBootstrap
   */
  def getNoArgSparkKernelActorSystem: ActorSystem =
    //if (actorSystem == null) { noArgKernelBootstrap; actorSystem }
    //else actorSystem
    if (actorSystem == null) { actorSystem = ActorSystem(); actorSystem }
    else actorSystem
}
