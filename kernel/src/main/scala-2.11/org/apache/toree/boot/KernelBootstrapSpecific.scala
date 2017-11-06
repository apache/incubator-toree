package org.apache.toree.boot

import org.apache.spark.repl.Main

/**
  * Provides Scala version-specific features needed for the [[KernelBootstrap]] class.
  */
private[boot] trait KernelBootstrapSpecific {
  protected val outputDir = Main.outputDir

  /**
    * Initializes all kernel systems.
    */
  def initialize() = {
    System.setProperty("spark.repl.class.outputDir", outputDir.getAbsolutePath)

    this
  }
}
