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
