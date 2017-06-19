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

package org.apache.toree.kernel.api

import java.io.{InputStream, PrintStream}
import java.net.URI
import com.typesafe.config.Config
import org.apache.spark.api.java.JavaSparkContext
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.sql.SparkSession
import org.apache.toree.interpreter.ExecuteOutput

/**
 * Interface for the kernel API. This does not include exposed variables.
 */
trait KernelLike {

  /**
   * Executes a block of code represented as a string and returns the result.
   *
   * @param code The code as an option to execute
   *
   * @return A tuple containing the result (true/false) and the output as a
   *         string
   */
  def eval(code: Option[String]): (Boolean, ExecuteOutput)

  /**
   * Returns a collection of methods that can be used to generate objects
   * related to the kernel.
   *
   * @return The collection of factory methods
   */
  def factory: FactoryMethodsLike

  /**
   * Returns a collection of methods that can be used to stream data from the
   * kernel to the client.
   *
   * @return The collection of stream methods
   */
  def stream: StreamMethodsLike

  /**
   * Returns a collection of methods that can be used to display data from the
   * kernel to the client.
   *
   * @return The collection of display methods
   */
  def display: DisplayMethodsLike

  /**
   * Returns a print stream to be used for communication back to clients
   * via standard out.
   *
   * @return The print stream instance or an error if the stream info is
   *         not found
   */
  def out: PrintStream

  /**
   * Returns a print stream to be used for communication back to clients
   * via standard error.
   *
   * @return The print stream instance or an error if the stream info is
   *         not found
   */
  def err: PrintStream

  /**
   * Returns an input stream to be used to receive information from the client.
   *
   * @return The input stream instance or an error if the stream info is
   *         not found
   */
  def in: InputStream

  /**
   * Represents data to be shared using the kernel as the middleman.
   *
   * @note Using Java structure to enable other languages to have easy access!
   */
  val data: java.util.Map[String, Any]


  def interpreter(name: String): Option[org.apache.toree.interpreter.Interpreter]

  def config: Config

  def addJars(uris: URI*)

  def sparkContext: SparkContext

  def sparkConf: SparkConf

  def javaSparkContext: JavaSparkContext

  def sparkSession: SparkSession
}
