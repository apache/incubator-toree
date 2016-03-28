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

package org.apache.toree.boot.layer

import java.io.File
import java.util.concurrent.ConcurrentHashMap

import akka.actor.ActorRef
import com.typesafe.config.Config
import org.apache.toree.comm.{CommManager, CommRegistrar, CommStorage, KernelCommManager}
import org.apache.toree.dependencies.{CoursierDependencyDownloader, DependencyDownloader}
import org.apache.toree.interpreter._
import org.apache.toree.kernel.api.Kernel
import org.apache.toree.kernel.protocol.v5.KMBuilder
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.magic.MagicManager
import org.apache.toree.plugins.PluginManager
import org.apache.toree.utils.LogLike

import scala.collection.JavaConverters._

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
    Kernel, DependencyDownloader, MagicManager, PluginManager,
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
    val pluginManager = createPluginManager(
      config, scalaInterpreter, dependencyDownloader)

    val kernel = initializeKernel(
        config, actorLoader, manager, commManager, pluginManager
    )

    initializeSparkContext(config, kernel, appName)

    manager.initializeInterpreters(kernel)

    val responseMap = initializeResponseMap()


    (commStorage, commRegistrar, commManager,
      manager.defaultInterpreter.orNull, kernel,
      dependencyDownloader, kernel.magics, pluginManager, responseMap)

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
    /*val dependencyDownloader = new IvyDependencyDownloader(
      "http://repo1.maven.org/maven2/", config.getString("ivy_local")
    )*/
    val dependencyDownloader = new CoursierDependencyDownloader
    dependencyDownloader.setDownloadDirectory(
      new File(config.getString("ivy_local"))
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
    pluginManager: PluginManager
  ) = {
    val kernel = new Kernel(
      config,
      actorLoader,
      interpreterManager,
      commManager,
      pluginManager
    )
    pluginManager.dependencyManager.add(kernel)

    kernel
  }

  private def createPluginManager(
    config: Config, interpreter: Interpreter,
    dependencyDownloader: DependencyDownloader
  ) = {
    logger.debug("Constructing plugin manager")
    val pluginManager = new PluginManager()

    logger.debug("Building dependency map")
    pluginManager.dependencyManager.add(interpreter)
    pluginManager.dependencyManager.add(dependencyDownloader)
    pluginManager.dependencyManager.add(config)

    pluginManager.dependencyManager.add(pluginManager)

    pluginManager
  }

  private def initializePlugins(
    config: Config,
    pluginManager: PluginManager
  ) = {
    val magicUrlArray = config.getStringList("magic_urls").asScala
      .map(s => new java.net.URL(s)).toArray

    if (magicUrlArray.isEmpty)
      logger.warn("No external magics provided to PluginManager!")
    else
      logger.info("Using magics from the following locations: " +
        magicUrlArray.map(_.getPath).mkString(","))

    // Load internal plugins under kernel module
    logger.debug("Loading internal plugins")
    val internalPlugins = pluginManager.initialize()
    logger.info(internalPlugins.size + " internal plugins loaded")

    // Load external plugins if provided
    logger.debug("Loading external plugins")
    val externalPlugins = if (magicUrlArray.nonEmpty) {
      val externalPlugins = pluginManager.loadPlugins(
        magicUrlArray.map(_.getFile).map(new File(_)): _*
      )
      pluginManager.initializePlugins(externalPlugins)
      externalPlugins
    } else Nil
    logger.info(externalPlugins.size + " external plugins loaded")
  }
}
