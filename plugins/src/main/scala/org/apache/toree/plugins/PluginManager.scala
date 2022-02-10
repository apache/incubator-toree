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
package org.apache.toree.plugins

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.apache.toree.plugins.dependencies._
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
 * Represents a manager of plugins to be loaded/executed/unloaded.
 *
 * @param pluginClassLoader The main classloader for loading plugins
 * @param pluginSearcher The search utility to find plugin classes
 * @param dependencyManager The dependency manager for plugins
 */
class PluginManager(
  private val pluginClassLoader: PluginClassLoader =
    new PluginClassLoader(Nil, classOf[PluginManager].getClassLoader),
  private val pluginSearcher: PluginSearcher = new PluginSearcher,
  val dependencyManager: DependencyManager = new DependencyManager
) {
  /** Represents logger used by plugin manager. */
  private val logger = LoggerFactory.getLogger(this.getClass)

  /** Represents internal plugins. */
  private lazy val internalPlugins: Map[String, Class[_]] =
    pluginSearcher.internal
      .map(_.name)
      .map(n => n -> pluginClassLoader.loadClass(n))
      .toMap

  /** Represents external plugins that can be loaded/unloaded. */
  private lazy val externalPlugins: collection.mutable.Map[String, Class[_]] =
    new ConcurrentHashMap[String, Class[_]]().asScala

  /** Represents all active (loaded and created) plugins. */
  private lazy val activePlugins: collection.mutable.Map[String, Plugin] =
    new ConcurrentHashMap[String, Plugin]().asScala

  /**
   * Returns whether or not the specified plugin is active.
   *
   * @param name The fully-qualified name of the plugin class
   * @return True if actively loaded, otherwise false
   */
  def isActive(name: String): Boolean = activePlugins.contains(name)

  /**
   * Returns a new iterator over active plugins contained by this manager.
   *
   * @return The iterator of active plugins
   */
  def plugins: Iterable[Plugin] = activePlugins.values

  /**
   * Initializes the plugin manager, performing the expensive task of searching
   * for all internal plugins, creating them, and initializing them.
   *
   * @return The collection of loaded plugins
   */
  def initialize(): Seq[Plugin] = {
    val newPlugins = internalPlugins.flatMap(t =>
      loadPlugin(t._1, t._2).toOption
    ).toSeq
    initializePlugins(newPlugins, DependencyManager.Empty)
    newPlugins
  }

  /**
   * Loads (but does not initialize) plugins from the provided paths.
   *
   * @param paths The file paths from which to load new plugins
   * @return The collection of loaded plugins
   */
  def loadPlugins(paths: File*): Seq[Plugin] = {
    require(paths.nonEmpty, "Plugin paths cannot be empty!")

    // Search for plugins in our new paths, then add loaded plugins to list
    // NOTE: Iterator returned from plugin searcher, so avoid building a
    //       large collection by performing all tasks together
    @volatile var newPlugins = collection.mutable.Seq[Plugin]()
    pluginSearcher.search(paths: _*).foreach(ci => {
      // Add valid path to class loader
      pluginClassLoader.addURL(ci.location.toURI.toURL)

      // Load class
      val klass = pluginClassLoader.loadClass(ci.name)

      // Add to external plugin list
      externalPlugins.put(ci.name, klass)

      // Load the plugin using the given name and class
      loadPlugin(ci.name, klass).foreach(newPlugins :+= _)
    })
    newPlugins
  }

  /**
   * Loads the plugin using the specified name.
   *
   * @param name The name of the plugin
   * @param klass The class representing the plugin
   * @return The new plugin instance if no plugin with the specified name
   *         exists, otherwise the plugin instance with the name
   */
  def loadPlugin(name: String, klass: Class[_]): Try[Plugin] = {
    if (isActive(name)) {
      logger.warn(s"Skipping $name as already actively loaded!")
      Success(activePlugins(name))
    } else {
      logger.debug(s"Loading $name as plugin")

      // Assume that each plugin has an empty constructor
      // https://docs.oracle.com/javase/9/docs/api/java/lang/Class.html#newInstance--
      val tryInstance = Try(klass.getDeclaredConstructor().newInstance())

      // Log failures
      tryInstance.failed.foreach(ex =>
        logger.error(s"Failed to load plugin $name", ex))

      // Attempt to cast as plugin type to add to active plugins
      tryInstance.transform({
        case p: Plugin  =>
          p.internalPluginManager_=(this)
          activePlugins.put(p.name, p)
          Success(p)
        case x          =>
          val name = x.getClass.getName
          logger.warn(s"Unknown plugin type '$name', ignoring!")
          Failure(new UnknownPluginTypeException(name))
      }, f => Failure(f))
    }
  }

  /**
   * Initializes a collection of plugins that may/may not have
   * dependencies on one another.
   *
   * @param plugins The collection of plugins to initialize
   * @param scopedDependencyManager The dependency manager containing scoped
   *                                dependencies to use over global ones
   * @return The collection of results in order of priority (higher to lower)
   */
  def initializePlugins(
    plugins: Seq[Plugin],
    scopedDependencyManager: DependencyManager = DependencyManager.Empty
  ): Seq[PluginMethodResult] = {
    val pluginMethods = plugins.flatMap(_.initMethods)
    val results = invokePluginMethods(
      pluginMethods,
      scopedDependencyManager
    )

    // Mark success/failure
    results.groupBy(_.pluginName).foreach { case (pluginName, g) =>
      val failures = g.flatMap(_.toTry.failed.toOption)
      val success = failures.isEmpty

      if (success) logger.debug(s"Successfully initialized plugin $pluginName!")
      else logger.warn(s"Initialization failed for plugin $pluginName!")

      // Log any specific failures for the plugin
      failures.foreach(ex => logger.error(pluginName, ex))
    }

    results
  }

  /**
   * Destroys a collection of plugins that may/may not have
   * dependencies on one another.
   *
   * @param plugins The collection of plugins to destroy
   * @param scopedDependencyManager The dependency manager containing scoped
   *                                dependencies to use over global ones
   * @param destroyOnFailure If true, destroys the plugin even if its destroy
   *                         callback fails
   * @return The collection of results in order of priority (higher to lower)
   */
  def destroyPlugins(
    plugins: Seq[Plugin],
    scopedDependencyManager: DependencyManager = DependencyManager.Empty,
    destroyOnFailure: Boolean = true
  ): Seq[PluginMethodResult] = {
    val pluginMethods = plugins.flatMap(_.destroyMethods)
    val results = invokePluginMethods(
      pluginMethods,
      scopedDependencyManager
    )

    // Perform check to remove destroyed plugins
    results.groupBy(_.pluginName).foreach { case (pluginName, g) =>
      val failures = g.flatMap(_.toTry.failed.toOption)
      val success = failures.isEmpty

      if (success) logger.debug(s"Successfully destroyed plugin $pluginName!")
      else if (destroyOnFailure) logger.debug(
        s"Failed to invoke some teardown methods, but destroyed plugin $pluginName!"
      )
      else logger.warn(s"Failed to destroy plugin $pluginName!")

      // If successful or forced, remove the plugin from our active list
      if (success || destroyOnFailure) activePlugins.remove(pluginName)

      // Log any specific failures for the plugin
      failures.foreach(ex => logger.error(pluginName, ex))
    }

    results
  }

  /**
   * Finds a plugin with the matching name.
   *
   * @param name The fully-qualified class name of the plugin
   * @return Some plugin if found, otherwise None
   */
  def findPlugin(name: String): Option[Plugin] = plugins.find(_.name == name)

  /**
   * Sends an event to all plugins actively listening for that event and
   * returns the first result, which is based on highest priority.
   *
   * @param eventName The name of the event
   * @param scopedDependencies The dependencies to provide directly to event
   *                           handlers
   * @return The first result from all plugin methods that executed the event
   */
  def fireEventFirstResult(
    eventName: String,
    scopedDependencies: Dependency[_ <: AnyRef]*
  ): Option[PluginMethodResult] = {
    fireEvent(eventName, scopedDependencies: _*).headOption
  }

  /**
   * Sends an event to all plugins actively listening for that event and
   * returns the last result, which is based on highest priority.
   *
   * @param eventName The name of the event
   * @param scopedDependencies The dependencies to provide directly to event
   *                           handlers
   * @return The last result from all plugin methods that executed the event
   */
  def fireEventLastResult(
    eventName: String,
    scopedDependencies: Dependency[_ <: AnyRef]*
  ): Option[PluginMethodResult] = {
    fireEvent(eventName, scopedDependencies: _*).lastOption
  }

  /**
   * Sends an event to all plugins actively listening for that event.
   *
   * @param eventName The name of the event
   * @param scopedDependencies The dependencies to provide directly to event
   *                           handlers
   * @return The collection of results in order of priority (higher to lower)
   */
  def fireEvent(
    eventName: String,
    scopedDependencies: Dependency[_ <: AnyRef]*
  ): Seq[PluginMethodResult] = {
    val dependencyManager = new DependencyManager
    scopedDependencies.foreach(d => dependencyManager.add(d))
    fireEvent(eventName, dependencyManager)
  }

  /**
   * Sends an event to all plugins actively listening for that event.
   *
   * @param eventName The name of the event
   * @param scopedDependencyManager The dependency manager containing scoped
   *                                dependencies to use over global ones
   * @return The collection of results in order of priority (higher to lower)
   */
  def fireEvent(
    eventName: String,
    scopedDependencyManager: DependencyManager = DependencyManager.Empty
  ): Seq[PluginMethodResult] = {
    val methods = plugins.flatMap(_.eventMethodMap.getOrElse(eventName, Nil))

    invokePluginMethods(methods.toSeq, scopedDependencyManager)
  }

  /**
   * Attempts to invoke all provided plugin methods. This is a naive
   * implementation that continually invokes bundles until either all bundles
   * are complete or failures are detected (needing dependencies that other
   * bundles do not provide).
   *
   * @param pluginMethods The collection of plugin methods to invoke
   * @param scopedDependencyManager The dependency manager containing scoped
   *                                dependencies to use over global ones
   * @return The collection of results in order of priority
   */
  private def invokePluginMethods(
    pluginMethods: Seq[PluginMethod],
    scopedDependencyManager: DependencyManager
  ): Seq[PluginMethodResult] = {
    // Continue trying to invoke plugins until we finish them all or
    // we reach a state where no plugin can be completed
    val completedMethods = Array.ofDim[PluginMethodResult](pluginMethods.size)

    // Sort by method priority and then, for ties, plugin priority
    @volatile var remainingMethods = prioritizePluginMethods(pluginMethods)

    @volatile var done = false
    while (!done) {
      // NOTE: Performing this per iteration as the global dependency manager
      //       can be updated by plugins with each invocation
      val dm = dependencyManager.merge(scopedDependencyManager)

      // Process all methods, adding any successful to completed and leaving
      // any failures to be processed again
      val newRemainingMethods = remainingMethods.map { case (m, i) =>
        val result = m.invoke(dm)
        if (result.isSuccess) completedMethods.update(i, result)
        (m, i, result)
      }.filter(_._3.isFailure)

      // If no change detected, we have failed to process all methods
      if (remainingMethods.size == newRemainingMethods.size) {
        // Place last failure for each method in our completed list
        newRemainingMethods.foreach { case (_, i, r) =>
          completedMethods.update(i, r)
        }
        done = true
      } else {
        // Update remaining methods to past failures
        remainingMethods = newRemainingMethods.map(t => (t._1, t._2))
        done = remainingMethods.isEmpty
      }
    }

    completedMethods
  }

  /**
   * Sorts plugin methods based on method priority and plugin priority.
   *
   * @param pluginMethods The collection of plugin methods to sort
   * @return The sorted plugin methods
   */
  private def prioritizePluginMethods(pluginMethods: Seq[PluginMethod]) =
    pluginMethods
      .groupBy(_.priority)
      .flatMap(_._2.sortWith(_.plugin.priority > _.plugin.priority))
      .toSeq
      .sortWith(_.priority > _.priority)
      .zipWithIndex
}
