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

import scala.util.{Failure, Success, Try}

/**
 * Represents a result from executing a plugin method.
 */
sealed trait PluginMethodResult {
  /** Represents the name of the plugin from which the result originated. */
  lazy val pluginName: String = pluginMethod.plugin.name

  /** Represents the name of the method from which the result originated. */
  lazy val methodName: String = pluginMethod.method.getName

  /** Represents the priority of the plugin from which the result originated. */
  lazy val pluginPriority: Long = pluginMethod.plugin.priority

  /** Represents the priority of the method from which the result originated. */
  lazy val methodPriority: Long = pluginMethod.priority

  /** Indicates whether or not this result is a success. */
  lazy val isSuccess: Boolean = toTry.isSuccess

  /** Indicates whether or not this result is a failure. */
  lazy val isFailure: Boolean = toTry.isFailure

  /** Represents the plugin method instance from which the result originated. */
  val pluginMethod: PluginMethod

  /** Converts result to a try. */
  def toTry: Try[AnyRef]
}

/**
 * A successful result from executing a plugin method.
 *
 * @param pluginMethod The method that was executed
 * @param result The result from the execution
 */
case class SuccessPluginMethodResult(
  pluginMethod: PluginMethod,
  result: AnyRef
) extends PluginMethodResult {
  val toTry: Try[AnyRef] = Success(result)
}

/**
 * A failed result from executing a plugin method.
 *
 * @param pluginMethod The method that was executed
 * @param throwable The error that was thrown
 */
case class FailurePluginMethodResult(
  pluginMethod: PluginMethod,
  throwable: Throwable
) extends PluginMethodResult {
  val toTry: Try[AnyRef] = Failure(throwable)
}

