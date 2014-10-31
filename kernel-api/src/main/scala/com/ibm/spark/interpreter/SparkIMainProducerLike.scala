package com.ibm.spark.interpreter

import org.apache.spark.repl.SparkIMain

import scala.tools.nsc.interpreter.JPrintWriter
import scala.tools.nsc.{Settings, interpreter}

trait SparkIMainProducerLike {
  /**
   * Constructs a new instance of SparkIMain.
   *
   * @param settings The settings associated with the SparkIMain instance
   * @param out The output writer associated with the SparkIMain instance
   *
   * @return The new SparkIMain instance
   */
  def newSparkIMain(settings: Settings, out: interpreter.JPrintWriter): SparkIMain
}

trait StandardSparkIMainProducer extends SparkIMainProducerLike {
  override def newSparkIMain(
    settings: Settings, out: JPrintWriter
  ): SparkIMain = new SparkIMain(settings, out)
}