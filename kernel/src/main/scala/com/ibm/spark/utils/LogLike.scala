package com.ibm.spark.utils

import org.slf4j.LoggerFactory

// TODO: Migrate this duplicate code (in client and core) to separate project

/**
 * A trait for mixing in logging. This trait
 * exposes a {@link org.slf4j.Logger}
 * through a protected field called logger
 */
trait LogLike {
  val loggerName = this.getClass.getName
  protected val logger = LoggerFactory.getLogger(loggerName)
}
