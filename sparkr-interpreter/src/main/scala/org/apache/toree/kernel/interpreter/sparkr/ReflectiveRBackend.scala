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
package org.apache.toree.kernel.interpreter.sparkr

/**
 * Provides reflective access into the backend R component that is not
 * publically accessible.
 */
class ReflectiveRBackend {
  private val rBackendClass = Class.forName("org.apache.spark.api.r.RBackend")
  private val rBackendInstance = rBackendClass.newInstance()

  /**
   * Initializes the underlying RBackend service.
   *
   * @return The port used by the service
   */
  def init(): Int = {
    val runMethod = rBackendClass.getDeclaredMethod("init")

    runMethod.invoke(rBackendInstance).asInstanceOf[Int]
  }

  /** Blocks until the service has finished. */
  def run(): Unit = {
    val runMethod = rBackendClass.getDeclaredMethod("run")

    runMethod.invoke(rBackendInstance)
  }

  /** Closes the underlying RBackend service. */
  def close(): Unit = {
    val runMethod = rBackendClass.getDeclaredMethod("close")

    runMethod.invoke(rBackendInstance)
  }
}
