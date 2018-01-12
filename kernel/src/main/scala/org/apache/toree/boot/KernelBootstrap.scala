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

package org.apache.toree.boot

import akka.actor.{ActorRef, ActorSystem}
import com.typesafe.config.Config
import org.apache.toree.boot.layer._
import org.apache.toree.interpreter.Interpreter
import org.apache.toree.kernel.api.Kernel
import org.apache.toree.kernel.protocol.v5.KernelStatusType._
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.security.KernelSecurityManager
import org.apache.toree.utils.LogLike

import org.apache.spark.repl.Main

import org.zeromq.ZMQ

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Try

class KernelBootstrap(config: Config) extends LogLike {
  this: BareInitialization with ComponentInitialization
    with HandlerInitialization with HookInitialization =>

  private val DefaultActorSystemName            = "spark-kernel-actor-system"

  private var actorSystem: ActorSystem          = _
  private var actorLoader: ActorLoader          = _
  private var kernelMessageRelayActor: ActorRef = _
  private var statusDispatch: ActorRef          = _
  private var kernel: Kernel                    = _

  private var interpreters: Seq[Interpreter]    = Nil
  private val rootDir                           = Main.rootDir
  private val outputDir                         = Main.outputDir

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

    // ENSURE THAT WE SET THE RIGHT SPARK PROPERTIES
    val execUri = System.getenv("SPARK_EXECUTOR_URI")
    System.setProperty("spark.repl.class.outputDir", outputDir.getAbsolutePath)
    if (execUri != null) {
      System.setProperty("spark.executor.uri", execUri)
    }

    displayVersionInfo()

    // Do this first to support shutting down quickly before entire system
    // is ready
    initializeShutdownHook()

    // Initialize the bare minimum to report a starting message
    val (actorSystem, actorLoader, kernelMessageRelayActor, statusDispatch) =
      initializeBare(
        config = config,
        actorSystemName = DefaultActorSystemName
      )

    this.actorSystem = actorSystem
    this.actorLoader = actorLoader
    this.kernelMessageRelayActor = kernelMessageRelayActor
    this.statusDispatch = statusDispatch

    // Indicate that the kernel is now starting
    publishStatus(KernelStatusType.Starting)

    // Initialize components needed elsewhere
    val (commStorage, commRegistrar, commManager, interpreter,
      kernel, dependencyDownloader,
      magicManager, pluginManager, responseMap) =
      initializeComponents(
        config      = config,
        actorLoader = actorLoader
      )
    this.interpreters ++= Seq(interpreter)

    this.kernel = kernel

    // Initialize our handlers that take care of processing messages
    initializeHandlers(
      actorSystem   = actorSystem,
      actorLoader   = actorLoader,
      kernel        = kernel,
      interpreter   = interpreter,
      commRegistrar = commRegistrar,
      commStorage   = commStorage,
      pluginManager = pluginManager,
      magicManager = magicManager,
      responseMap   = responseMap
    )

    // Initialize our non-shutdown hooks that handle various JVM events
    initializeHooks(
      config = config,
      interpreter = interpreter
    )

    logger.debug("Initializing security manager")
    System.setSecurityManager(new KernelSecurityManager)

    logger.debug("Running postInit for interpreters")
    interpreters foreach {_.postInit()}

    logger.info("Marking relay as ready for receiving messages")
    kernelMessageRelayActor ! true

    this
  }

  /**
   * Shuts down all kernel systems.
   */
  def shutdown() = {
    logger.info("Shutting down interpreters")
    Try(interpreters.foreach(_.stop())).failed.foreach(
      logger.error("Failed to shutdown interpreters", _: Throwable)
    )

    logger.info("Shutting down actor system")
    Try(actorSystem.terminate()).failed.foreach(
      logger.error("Failed to shutdown actor system", _: Throwable)
    )

    this
  }

  /**
   * Waits for the main actor system to terminate.
   */
  def waitForTermination() = {
    logger.debug("Waiting for actor system to terminate")
//    actorSystem.awaitTermination()
    Await.result(actorSystem.whenTerminated, Duration.Inf)

    this
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

  @inline private def displayVersionInfo() = {
    logger.info("Kernel version: " + SparkKernelInfo.implementationVersion)
    logger.info("Scala version: " + SparkKernelInfo.scalaVersion)
    logger.info("ZeroMQ (JeroMQ) version: " + ZMQ.getVersionString)
  }
}
