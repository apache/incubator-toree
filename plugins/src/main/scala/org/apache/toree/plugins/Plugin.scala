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

import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

import org.apache.toree.annotations.Internal

import scala.reflect.runtime.universe.TypeTag

/**
 * Contains constants for the plugin interface.
 */
object Plugin {
  /** Default priority for a plugin if not marked explicitly. */
  val DefaultPriority: Long = 0
}

/**
 * Represents the generic plugin interface.
 */
trait Plugin {
  /** Plugin manager containing the plugin */
  @Internal private var _internalPluginManager: PluginManager = null

  /** Represents the fully qualified name of the plugin. */
  final val name: String = getClass.getName

  /**
   * Represents the simple name of the plugin. In the case of a anonymous class
   * it will just be the name of the anonymous class.
   */
  final val simpleName: String = getClass.getSimpleName

  /** Represents the priority of the plugin. */
  final val priority: Long = {
    Option(getClass.getAnnotation(classOf[annotations.Priority]))
      .map(_.level())
      .getOrElse(Plugin.DefaultPriority)
  }

  /** Sets the plugin manager pointer for this plugin. */
  @Internal private[plugins] final def internalPluginManager_=(
    _pluginManager: PluginManager
  ) = {
    require(
      this._internalPluginManager == null,
      "Plugin manager cannot be reassigned!"
    )
    this._internalPluginManager = _pluginManager
  }

  /** Returns the plugin manager pointer for this plugin. */
  @Internal private[plugins] final def internalPluginManager =
    _internalPluginManager

  /** Represents all @init methods in the plugin. */
  @Internal private[plugins] final lazy val initMethods: Seq[PluginMethod] = {
    allMethods.filter(_.isInit)
  }

  /** Represents all @destroy methods in the plugin. */
  @Internal private[plugins] final lazy val destroyMethods: Seq[PluginMethod] = {
    allMethods.filter(_.isDestroy)
  }

  /** Represents all @event methods in the plugin. */
  @Internal private[plugins] final lazy val eventMethods: Seq[PluginMethod] = {
    allMethods.filter(_.isEvent)
  }

  /** Represents all @events methods in the plugin. */
  @Internal private[plugins] final lazy val eventsMethods: Seq[PluginMethod] = {
    allMethods.filter(_.isEvents)
  }

  /** Represents all public/protected methods contained by this plugin. */
  private final lazy val allMethods: Seq[PluginMethod] =
    getClass.getMethods.map(PluginMethod.apply(this, _: Method))

  /** Represents mapping of event names to associated plugin methods. */
  @Internal private[plugins] final lazy val eventMethodMap: Map[String, Seq[PluginMethod]] = {
    val allEventMethods = (eventMethods ++ eventsMethods).distinct
    val allEventNames = allEventMethods.flatMap(_.eventNames).distinct
    allEventNames.map(name =>
      name -> allEventMethods.filter(_.eventNames.contains(name))
    ).toMap
  }

  /**
   * Registers a new dependency to be associated with this plugin.
   *
   * @param value The value of the dependency
   * @tparam T The dependency's type
   */
  protected def register[T <: AnyRef : TypeTag](value: T): Unit = {
    register(java.util.UUID.randomUUID().toString, value)
  }

  /**
   * Registers a new dependency to be associated with this plugin.
   *
   * @param name The name of the dependency
   * @param value The value of the dependency
   * @param typeTag The type information for the dependency
   * @tparam T The dependency's type
   */
  protected def register[T <: AnyRef](name: String, value: T)(implicit typeTag: TypeTag[T]): Unit = {
    assert(_internalPluginManager != null, "Internal plugin manager reference invalid!")
    _internalPluginManager.dependencyManager.add(name, value)
  }
}
