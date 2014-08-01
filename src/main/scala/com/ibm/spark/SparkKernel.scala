package com.ibm.spark

import java.io.{FileOutputStream, PrintWriter, File}
import com.ibm.spark.interpreter.ScalaInterpreter
import com.ibm.spark.kernel.protocol.v5.socket._
import org.apache.spark.{SparkConf, SparkContext}

object SparkKernel extends App {
  private val options = new SparkKernelOptions(args)

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

  // Create socket actors
  /*
  private val socketConfigReader = new SocketConfigReader(options.profile)
  private val socketFactory = new SocketFactory(socketConfigReader.getSocketConfig)
  private val heartbeatActor = new Heartbeat(socketFactory)
  private val shellActor = new Shell(socketFactory)
  private val ioPubActor = new IOPub(socketFactory)
  */

  /** TESTING */
  val intp = new ScalaInterpreter(options.tail, Console.out)
  //val intp = new ScalaInterpreter(options.tail, Console.out)
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
  import scala.tools.nsc.interpreter._
  {
    val (result, output) = intp.interpret( """val count = sc.parallelize(1 to 10).count()""")
    result match {
      case IR.Success => println("Success: " + output.trim)
      case _ => println("Something went wrong: " + output.trim)
    }
  }
  {
    val (result, output) = intp.interpret( """Console.println("\"Console.println(Count is " + count + ")\"")""")
    result match {
      case IR.Success => println("Success: " + output.trim)
      case _ => println("Something went wrong: " + output.trim)
    }
  }
  {
    val (result, output) = intp.interpret( """println("\"println(Count is " + count + ")\"")""")
    result match {
      case IR.Success => println("Success: " + output.trim)
      case _ => println("Something went wrong: " + output.trim)
    }
  }
  {
    val (result, output) = intp.interpret( """Console.println(Console.BLACK)""")
    result match {
      case IR.Success => println("Success: " + output.trim)
      case _ => println("Something went wrong: " + output.trim)
    }
  }
  {
    val (result, output) = intp.interpret( """Console.potato""")
    result match {
      case IR.Success => println("Success: " + output.trim)
      case _ => println("Something went wrong: " + output.trim)
    }
  }
  {
    val (result, output) = intp.interpret( """System.out.println("System.out.println")""")
    result match {
      case IR.Success => println("Success: " + output.trim)
      case _ => println("Something went wrong: " + output.trim)
    }
  }

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
