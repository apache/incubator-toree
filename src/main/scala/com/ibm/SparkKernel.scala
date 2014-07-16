package com.ibm

object SparkKernel extends App {
  val options = new SparkKernelOptions(args)

  if (options.help) {
    options.printHelpOn(System.out)
  } else {
    println("Starting kernel!")

    // TODO: Perform kernel setup and run it
    if (options.create_context) println("Creating a new context!")
    if (options.verbose) println("Running verbosely!")
    options.profile match {
      case Some(_) => println("Received a profile!")
      case None => println("No profile received!")
    }
  }
}
