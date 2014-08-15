package com.ibm.spark.magic.dependencies

import com.ibm.spark.interpreter.Interpreter
import org.apache.spark.SparkContext

import scala.reflect.runtime.universe._
import com.ibm.spark.magic.builtin.MagicTemplate

/**
 * Represents a mapping of dependency types to implementations.
 *
 * TODO: Explore Scala macros to avoid duplicate code.
 */
class DependencyMap {
  val internalMap = scala.collection.mutable.Map[Type, PartialFunction[MagicTemplate, Unit]]()

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
          magic.asInstanceOf[IncludeSparkContext].sparkContext = (sparkContext)
      )

    this
  }
}
