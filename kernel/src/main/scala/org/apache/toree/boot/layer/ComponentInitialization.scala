/*
 * Copyright 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.toree.boot.layer

import java.util
import java.util.concurrent.ConcurrentHashMap

import akka.actor.ActorRef
import org.apache.toree.comm.{CommManager, KernelCommManager, CommRegistrar, CommStorage}
import org.apache.toree.dependencies.{DependencyDownloader, IvyDependencyDownloader}
import org.apache.toree.global
import org.apache.toree.interpreter._
import org.apache.toree.kernel.api.{KernelLike, Kernel}
import org.apache.toree.kernel.protocol.v5.KMBuilder
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.kernel.protocol.v5.stream.KernelOutputStream
import org.apache.toree.magic.MagicLoader
import org.apache.toree.magic.builtin.BuiltinLoader
import org.apache.toree.magic.dependencies.DependencyMap
import org.apache.toree.utils.{MultiClassLoader, TaskManager, KeyValuePairUtils, LogLike}
import com.typesafe.config.Config
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkContext, SparkConf}

import scala.collection.JavaConverters._

import scala.util.Try

/**
 * Represents the component initialization. All component-related pieces of the
 * kernel (non-actors) should be created here. Limited items should be exposed.
 */
trait ComponentInitialization {
  /**
   * Initializes and registers all components (not needed by bare init).
   *
   * @param config The config used for initialization
   * @param appName The name of the "application" for Spark
   * @param actorLoader The actor loader to use for some initialization
   */
  def initializeComponents(
    config: Config, appName: String, actorLoader: ActorLoader
  ): (CommStorage, CommRegistrar, CommManager, Interpreter,
    Kernel, DependencyDownloader, MagicLoader,
    collection.mutable.Map[String, ActorRef])
}

/**
 * Represents the standard implementation of ComponentInitialization.
 */
trait StandardComponentInitialization extends ComponentInitialization {
  this: LogLike =>

  /**
   * Initializes and registers all components (not needed by bare init).
   *
   * @param config The config used for initialization
   * @param appName The name of the "application" for Spark
   * @param actorLoader The actor loader to use for some initialization
   */
  def initializeComponents(
    config: Config, appName: String, actorLoader: ActorLoader
  ) = {
    val (commStorage, commRegistrar, commManager) =
      initializeCommObjects(actorLoader)

    val manager =  InterpreterManager(config)
    val scalaInterpreter = manager.interpreters.get("Scala").orNull

    val dependencyDownloader = initializeDependencyDownloader(config)
    val magicLoader = initializeMagicLoader(
      config, scalaInterpreter, dependencyDownloader)

    val kernel = initializeKernel(
      config, actorLoader, manager, commManager, magicLoader
    )

    val responseMap = initializeResponseMap()

    initializeSparkContext(config, kernel, appName)

    (commStorage, commRegistrar, commManager,
      manager.defaultInterpreter.orNull, kernel,
      dependencyDownloader, magicLoader, responseMap)

  }


  def initializeSparkContext(config:Config, kernel:Kernel, appName:String) = {
    if(!config.getBoolean("nosparkcontext")) {
      kernel.createSparkContext(config.getString("spark.master"), appName)
    }
  }

  private def initializeCommObjects(actorLoader: ActorLoader) = {
    logger.debug("Constructing Comm storage")
    val commStorage = new CommStorage()

    logger.debug("Constructing Comm registrar")
    val commRegistrar = new CommRegistrar(commStorage)

    logger.debug("Constructing Comm manager")
    val commManager = new KernelCommManager(
      actorLoader, KMBuilder(), commRegistrar)

    (commStorage, commRegistrar, commManager)
  }

  private def initializeDependencyDownloader(config: Config) = {
    val dependencyDownloader = new IvyDependencyDownloader(
      "http://repo1.maven.org/maven2/", config.getString("ivy_local")
    )

    dependencyDownloader
  }

  protected def initializeResponseMap(): collection.mutable.Map[String, ActorRef] =
    new ConcurrentHashMap[String, ActorRef]().asScala

  private def initializeKernel(
    config: Config,
    actorLoader: ActorLoader,
    interpreterManager: InterpreterManager,
    commManager: CommManager,
    magicLoader: MagicLoader
  ) = {
    val kernel = new Kernel(
      config,
      actorLoader,
      interpreterManager,
      commManager,
      magicLoader
    )
    /*
    interpreter.doQuietly {
      interpreter.bind(
        "kernel", "org.apache.toree.kernel.api.Kernel",
        kernel, List( """@transient implicit""")
      )
    }
    */
    magicLoader.dependencyMap.setKernel(kernel)

    kernel
  }

  private def initializeMagicLoader(
    config: Config, interpreter: Interpreter,
    dependencyDownloader: DependencyDownloader
  ) = {
    logger.debug("Constructing magic loader")

    logger.debug("Building dependency map")
    val dependencyMap = new DependencyMap()
      .setInterpreter(interpreter)
      .setKernelInterpreter(interpreter) // This is deprecated
      .setDependencyDownloader(dependencyDownloader)
      .setConfig(config)

    logger.debug("Creating BuiltinLoader")
    val builtinLoader = new BuiltinLoader()

    val magicUrlArray = config.getStringList("magic_urls").asScala
      .map(s => new java.net.URL(s)).toArray

    if (magicUrlArray.isEmpty)
      logger.warn("No external magics provided to MagicLoader!")
    else
      logger.info("Using magics from the following locations: " +
        magicUrlArray.map(_.getPath).mkString(","))

    val multiClassLoader = new MultiClassLoader(
      builtinLoader,
      interpreter.classLoader
    )

    logger.debug("Creating MagicLoader")
    val magicLoader = new MagicLoader(
      dependencyMap = dependencyMap,
      urls = magicUrlArray,
      parentLoader = multiClassLoader
    )
    magicLoader.dependencyMap.setMagicLoader(magicLoader)
    magicLoader
  }
}
