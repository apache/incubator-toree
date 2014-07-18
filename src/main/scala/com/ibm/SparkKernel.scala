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

  //lazy val urls = java.lang.Thread.currentThread.getContextClassLoader match {
  //  case cl: java.net.URLClassLoader => cl.getURLs.toList
  //  case _ => error("classloader is not a URLClassLoader")
  //}
  //lazy val classpath = urls map {_.toString}

  //val loop = new SparkILoop()
  //val settings: Settings = new SparkCommandLine(options.tail).settings


  //settings.usejavacp.value = true
  //settings.classpath.value = classpath.distinct.mkString(java.io.File.pathSeparator)
  //loop.process(settings)

  /** TESTING */
  val settings: Settings = new SparkCommandLine(options.tail).settings

  {
    val cl = this.getClass.getClassLoader // or getClassLoader.getParent, or one more getParent...

    val urls = cl match {
      case cl: java.net.URLClassLoader => cl.getURLs.toList
      case a => sys.error("oops: I was expecting an URLClassLoader, foud a " + a.getClass)
    }
    val classpath = urls map {
      _.toString
    }

    settings.classpath.value = classpath.distinct.mkString(java.io.File.pathSeparator)
    settings.embeddedDefaults(cl) // or getClass.getClassLoader
  }

  /*
  settings.bootclasspath.value +=
    scala.tools.util.PathResolver.Environment.javaBootClassPath + File.pathSeparator + "lib/scala-library.jar"
  val sparkIMain: SparkIMain = new SparkIMain(settings, new JPrintWriter(Console.out, true)) {
    override protected def parentClassLoader = settings.getClass.getClassLoader()
  }
  */
  val sparkIMain: SparkIMain = new SparkIMain(settings, new JPrintWriter(Console.out, true))

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
         @transient val sc = com.ibm.SparkKernel.createSparkContext
            """)
    sparkIMain.interpret("import org.apache.spark.SparkContext._")
  }

  sparkIMain.interpret(
    """
      val count = sc.parallelize(1 to 10).count()
    """)

  sparkIMain.interpret("""
    println("Count = " + count)
    """)
}
