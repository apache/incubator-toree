package com.ibm.spark

object SparkKernel extends App {

  private val options = new SparkKernelOptions(args)

  if (options.help) {
    options.printHelpOn(System.out)
  } else {
    SparkKernelBootstrap(options.toConfig)
      .initialize()
      .waitForTermination()
      .shutdown()
  }
}
