/*
 * Copyright 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.toree.magic.dependencies

import java.io.OutputStream

import org.apache.toree.interpreter.Interpreter
import org.apache.toree.kernel.api.KernelLike
import org.apache.toree.magic.{MagicLoader, Magic}
import com.typesafe.config.Config
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext

import scala.reflect.runtime.universe._
import org.apache.toree.dependencies.DependencyDownloader

/**
 * Represents a mapping of dependency types to implementations.
 *
 * TODO: Explore Scala macros to avoid duplicate code.
 */
class DependencyMap {
  val internalMap =
    scala.collection.mutable.Map[Type, PartialFunction[Magic, Unit]]()

  /**
   * Sets the Interpreter for this map.
   * @param interpreter The new Interpreter
   */
  def setInterpreter(interpreter: Interpreter) = {
    internalMap(typeOf[IncludeInterpreter]) =
      PartialFunction[Magic, Unit](
        magic =>
          magic.asInstanceOf[IncludeInterpreter].interpreter_=(interpreter)
      )

    this
  }

  /**
   * Sets the Interpreter for this map.
   * @param interpreter The new Interpreter
   */
  //@deprecated("Use setInterpreter with IncludeInterpreter!", "2015.05.06")
  def setKernelInterpreter(interpreter: Interpreter) = {
    internalMap(typeOf[IncludeKernelInterpreter]) =
      PartialFunction[Magic, Unit](
        magic =>
          magic.asInstanceOf[IncludeKernelInterpreter].kernelInterpreter_=(interpreter)
      )

    this
  }

  /**
   * Sets the SparkContext for this map.
   * @param sparkContext The new SparkContext
   */
  def setSparkContext(sparkContext: SparkContext) = {
    internalMap(typeOf[IncludeSparkContext]) =
      PartialFunction[Magic, Unit](
        magic =>
          magic.asInstanceOf[IncludeSparkContext].sparkContext_=(sparkContext)
      )

    this
  }
  
  /**
   * Sets the SQLContext for this map.
   * @param sqlContext The new SQLContext
   */
  def setSQLContext(sqlContext: SQLContext) = {
    internalMap(typeOf[IncludeSQLContext]) =
      PartialFunction[Magic, Unit](
        magic =>
          magic.asInstanceOf[IncludeSQLContext].sqlContext_=(sqlContext)
      )

    this
  }

  /**
   * Sets the OutputStream for this map.
   * @param outputStream The new OutputStream
   */
  def setOutputStream(outputStream: OutputStream) = {
    internalMap(typeOf[IncludeOutputStream]) =
      PartialFunction[Magic, Unit](
        magic =>
          magic.asInstanceOf[IncludeOutputStream].outputStream_=(outputStream)
      )

    this
  }

  /**
   * Sets the DependencyDownloader for this map.
   * @param dependencyDownloader The new DependencyDownloader
   */
  def setDependencyDownloader(dependencyDownloader: DependencyDownloader) = {
    internalMap(typeOf[IncludeDependencyDownloader]) =
      PartialFunction[Magic, Unit](
        magic =>
          magic.asInstanceOf[IncludeDependencyDownloader]
            .dependencyDownloader_=(dependencyDownloader)
      )

    this
  }  
  
  /**
   * Sets the Kernel Object for this map.
   * @param kernel The new Kernel
   */
  def setKernel(kernel: KernelLike) = {
    internalMap(typeOf[IncludeKernel]) =
      PartialFunction[Magic, Unit](
        magic =>
          magic.asInstanceOf[IncludeKernel]
            .kernel_=(kernel)
      )

    this
  }

  /**
   * Sets the MagicLoader for this map.
   * @param magicLoader The new MagicLoader
   */
  def setMagicLoader(magicLoader: MagicLoader) = {
    internalMap(typeOf[IncludeMagicLoader]) =
      PartialFunction[Magic, Unit](
        magic =>
          magic.asInstanceOf[IncludeMagicLoader]
            .magicLoader_=(magicLoader)
      )

    this
  }

  /**
   * Sets the Config Object for this map.
   * @param config The config for the kernel
   */
  def setConfig(config: Config) = {
    internalMap(typeOf[IncludeConfig]) =
      PartialFunction[Magic, Unit](
        magic =>
          magic.asInstanceOf[IncludeConfig]
            .config=(config)
      )

    this
  }
}
