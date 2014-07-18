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
      case Some(_) => println("Received a profile!")
      case None => println("No profile received!")
    }
  }

  /** TESTING */
  val settings: Settings = new SparkCommandLine(options.tail).settings
  settings.bootclasspath.value +=
    scala.tools.util.PathResolver.Environment.javaBootClassPath + File.pathSeparator + "lib/scala-library.jar"
  val sparkIMain: SparkIMain = new SparkIMain(settings, new JPrintWriter(Console.out, true)) {
    override protected def parentClassLoader = settings.getClass.getClassLoader()
  }

  def createSparkContext: SparkContext = {
    val execUri = System.getenv("SPARK_EXECUTOR_URI")
    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("Spark shell")
      //.setJars(jars)
      .set("spark.repl.class.uri", sparkIMain.classServer.uri)
    if (execUri != null) {
      conf.set("spark.executor.uri", execUri)
    }
    if (System.getenv("SPARK_HOME") != null) {
      conf.setSparkHome(System.getenv("SPARK_HOME"))
    }
    val sparkContext = new SparkContext(conf)
    sparkContext
  }

  sparkIMain.initializeSynchronous()

  sparkIMain.beQuietDuring {
    sparkIMain.interpret("""
         @transient val sc = com.ibm.SparkKernel.createSparkContext();
            """)
    sparkIMain.interpret("import org.apache.spark.SparkContext._")
  }

  sparkIMain.interpret(
    """
      val count = spark.parallelize(1 to 10).count()
    """)

  sparkIMain.interpret("""
    println("Count = " + count)
    """)
}
