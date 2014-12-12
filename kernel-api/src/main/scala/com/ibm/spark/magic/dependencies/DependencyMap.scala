/*
 * Copyright 2014 IBM Corp.
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

package com.ibm.spark.magic.dependencies

import java.io.OutputStream

import com.ibm.spark.interpreter.Interpreter
import com.ibm.spark.magic.MagicTemplate
import org.apache.spark.SparkContext

import scala.reflect.runtime.universe._
import com.ibm.spark.dependencies.DependencyDownloader

/**
 * Represents a mapping of dependency types to implementations.
 *
 * TODO: Explore Scala macros to avoid duplicate code.
 */
class DependencyMap {
  val internalMap =
    scala.collection.mutable.Map[Type, PartialFunction[MagicTemplate, Unit]]()

  /**
   * Sets the Interpreter for this map.
   * @param interpreter The new Interpreter
   */
  def setInterpreter(interpreter: Interpreter) = {
    internalMap(typeOf[IncludeInterpreter]) =
      PartialFunction[MagicTemplate, Unit](
        magic =>
          magic.asInstanceOf[IncludeInterpreter].interpreter_=(interpreter)
      )

    this
  }

  /**
   * Sets the SparkContext for this map.
   * @param sparkContext The new SparkContext
   */
  def setSparkContext(sparkContext: SparkContext) = {
    internalMap(typeOf[IncludeSparkContext]) =
      PartialFunction[MagicTemplate, Unit](
        magic =>
          magic.asInstanceOf[IncludeSparkContext].sparkContext_=(sparkContext)
      )

    this
  }

  /**
   * Sets the OutputStream for this map.
   * @param outputStream The new OutputStream
   */
  def setOutputStream(outputStream: OutputStream) = {
    internalMap(typeOf[IncludeOutputStream]) =
      PartialFunction[MagicTemplate, Unit](
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
      PartialFunction[MagicTemplate, Unit](
        magic =>
          magic.asInstanceOf[IncludeDependencyDownloader]
            .dependencyDownloader=(dependencyDownloader)
      )

    this
  }
}
