package com.ibm.spark

object SparkKernel extends App {
  private val options = new SparkKernelOptions(args)

  if (options.help) {
    options.printHelpOn(System.out)
  } else {
    SparkKernelBootstrap(options).initialize()

    Console.println("Ctrl-C to terminate this kernel!")
    while (true) Thread.sleep(1000)
  }
}
