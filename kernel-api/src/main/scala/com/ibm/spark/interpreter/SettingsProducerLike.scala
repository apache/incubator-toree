package com.ibm.spark.interpreter

import org.apache.spark.repl.SparkCommandLine

import scala.tools.nsc.Settings

trait SettingsProducerLike {
  /**
   * Creates a new Settings instance.
   *
   * @param args The list of command-line arguments to associate with Settings
   *
   * @return The new instance of Settings
   */
  def newSettings(args: List[String]): Settings
}

trait StandardSettingsProducer extends SettingsProducerLike {
  override def newSettings(args: List[String]): Settings =
    new SparkCommandLine(args).settings
}
