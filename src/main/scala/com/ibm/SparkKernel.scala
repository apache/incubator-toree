package com.ibm

import java.io.File

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.repl.{SparkILoop, SparkIMain, SparkCommandLine}

import scala.tools.nsc.interpreter.JPrintWriter
import scala.tools.nsc.{CompilerCommand, Settings}

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
      case Some(_) =>
        println("Loading ZeroMQ settings from profile!")
      case None =>
        println("No profile received! Using default ZeroMQ settings!")
    }
  }

  /** TESTING */
  val intp = new SparkInterpreter(options.tail)
  intp.start()
  intp.interpret("""val count = sc.parallelize(1 to 10).count()""")
  intp.interpret("""println("Count = " + count)""")

  // Configure our interpreter to shut down when the JVM shuts down
  // TODO: This does not work
  val mainThread = Thread.currentThread()
  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run() = {
      intp.stop()
      if (mainThread.isAlive) mainThread.join()
    }
  })

  Console.println("Ctrl-C to terminate this kernel!")
  while (true) Thread.sleep(1000)
}
