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

import java.lang.reflect.{InvocationTargetException, Method}

import org.apache.toree.plugins.annotations._
import org.apache.toree.plugins.dependencies._

import scala.util.Try

/**
 * Represents a method for a specific plugin
 *
 * @param plugin The plugin containing this method
 * @param method The method to invoke
 */
case class PluginMethod(
  plugin: Plugin,
  method: Method
) {
  /** Represents the collection of names of events this method supports. */
  lazy val eventNames: Seq[String] = {
    Option(method.getAnnotation(classOf[Event]))
      .map(_.name()).map(Seq(_)).getOrElse(Nil) ++
    Option(method.getAnnotation(classOf[Events]))
      .map(_.names()).map(_.toSeq).getOrElse(Nil)
  }

  /** Represents whether or not this method triggers on initialization. */
  lazy val isInit: Boolean = method.isAnnotationPresent(classOf[Init])

  /** Represents whether or not this method contains an Event annotation. */
  lazy val isEvent: Boolean = method.isAnnotationPresent(classOf[Event])

  /** Represents whether or not this method contains an Events annotation. */
  lazy val isEvents: Boolean = method.isAnnotationPresent(classOf[Events])

  /** Represents whether or not this method triggers on destruction. */
  lazy val isDestroy: Boolean = method.isAnnotationPresent(classOf[Destroy])

  /** Represents this method's priority. */
  lazy val priority: Long = Option(method.getAnnotation(classOf[Priority]))
    .map(_.level()).getOrElse(PluginMethod.DefaultPriority)

  /**
   * Invokes by loading all needed dependencies and providing them as
   * arguments to the method.
   *
   * @param dependencies The collection of dependencies to inject into the
   *                     method for its arguments (as needed)
   * @return The result from invoking the plugin
   */
  @throws[DepNameNotFoundException]
  @throws[DepClassNotFoundException]
  @throws[DepUnexpectedClassException]
  def invoke(dependencies: Dependency[_ <: AnyRef]*): PluginMethodResult = {
    invoke(DependencyManager.from(dependencies: _*))
  }

  /**
   * Invokes by loading all needed dependencies and providing them as
   * arguments to the method.
   *
   * @param dependencyManager The dependency manager containing dependencies
   *                          to inject into the method for its arguments
   *                          (as needed)
   * @return The result from invoking the plugin
   */
  def invoke(dependencyManager: DependencyManager): PluginMethodResult = Try({
    // Get dependency info (if has specific name or just use class)
    val depInfo = method.getParameterAnnotations
      .zip(method.getParameterTypes)
      .map { case (annotations, parameterType) =>
        (annotations.collect {
          case dn: DepName => dn
        }.lastOption.map(_.name()), parameterType)
      }

    // Load dependencies for plugin method
    val dependencies = depInfo.map { case (name, c) => name match {
      case Some(n) =>
        val dep = dependencyManager.find(n)
        if (dep.isEmpty) throw new DepNameNotFoundException(n)

        // Verify found dep has acceptable class
        val depClass: Class[_] = dep.get.valueClass
        if (!c.isAssignableFrom(depClass))
          throw new DepUnexpectedClassException(n, c, depClass)

        dep.get
      case None =>
        val deps = dependencyManager.findByValueClass(c)
        if (deps.isEmpty) throw new DepClassNotFoundException(c)
        deps.last
    } }

    // Validate arguments
    val arguments: Seq[AnyRef] = dependencies.map(_.value.asInstanceOf[AnyRef])

    // Invoke plugin method
    method.invoke(plugin, arguments: _*)
  }).map(SuccessPluginMethodResult.apply(this, _: AnyRef)).recover {
    case i: InvocationTargetException =>
      FailurePluginMethodResult(this, i.getTargetException)
    case throwable: Throwable =>
      FailurePluginMethodResult(this, throwable)
  }.get
}

object PluginMethod {
  /** Default priority for a plugin method if not marked explicitly. */
  val DefaultPriority: Long = 0
}
