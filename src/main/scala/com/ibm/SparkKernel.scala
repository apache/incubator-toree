package com.ibm

import java.io.File

import com.ibm.interpreter.ScalaInterpreter
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
      case Some(profileFile) =>
        println(s"Loading ZeroMQ settings from ${profileFile}!")
      case None =>
        println("No profile received! Using default ZeroMQ settings!")
    }
  }

  /** TESTING */
  val intp = new ScalaInterpreter(options.tail, Console.out)
  intp.start()

  val conf = new SparkConf()
  conf.setMaster("local[*]")
    .setAppName("IBM Spark Kernel")
    //.set("spark.executor.uri", "...")
    //.setSparkHome("...")
  intp.sparkIMain.beQuietDuring {
    //logger.info("Creating a new context identified by 'sc'")
    conf.set("spark.repl.class.uri", intp.sparkIMain.classServer.uri)
    intp.sparkIMain.bind(
      "sc", "org.apache.spark.SparkContext",
      new SparkContext(conf), List( """@transient"""))
  }

  // Run some code
  intp.interpret("""val count = sc.parallelize(1 to 10).count()""")
  intp.interpret("""println("Count = " + count)""")

  // Configure our interpreter to shut down when the JVM shuts down
  // TODO: This does not work
  val mainThread = Thread.currentThread()
  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run() = {
      intp.stop()
      // TODO: Check if you can magically access the spark context to stop it
      // TODO: inside a different thread
      if (mainThread.isAlive) mainThread.join()
    }
  })

  Console.println("Ctrl-C to terminate this kernel!")
  while (true) Thread.sleep(1000)
}
