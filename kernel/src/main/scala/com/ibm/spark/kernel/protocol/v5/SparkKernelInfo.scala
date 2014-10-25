package com.ibm.spark.kernel.protocol.v5

object SparkKernelInfo {
  /**
   * Represents the protocol version (IPython) supported by this kernel.
   */
  val protocolVersion         = "5.0"

  /**
   * Represents what the kernel implements.
   */
  val implementation          = "spark"

  /**
   * Represents the kernel version.
   */
  val implementationVersion   = "0.1.0"

  /**
   * Represents the language supported by the kernel.
   */
  val language                = "scala"

  /**
   * Represents the language version supported by the kernel.
   */
  val languageVersion         = "2.10.4"

  /**
   * Represents the displayed name of the kernel.
   */
  val banner                  = "IBM Spark Kernel"

  /**
   * Represents the name of the user who started the kernel process.
   */
  val username                = System.getProperty("user.name")

  /**
   * Represents the unique session id used by this instance of the kernel.
   */
  val session               = java.util.UUID.randomUUID.toString
}