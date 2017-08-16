package org.apache.toree.boot

/**
  * Provides Scala version-specific features needed for the [[KernelBootstrap]] class.
  */
private[boot] trait KernelBootstrapSpecific {
  /**
    * Initializes all kernel systems.
    */
  def initialize() = {
    this
  }
}
