package com.ibm

import org.apache.spark.repl.{SparkCommandLine, SparkIMain}
import org.apache.spark.{SparkConf, SparkContext}

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.{JPrintWriter, LoopCommands}

class SparkInterpreter(args: List[String]) {
  val settings: Settings = new SparkCommandLine(args).settings

  /* Add scala.runtime libraries to interpreter classpath */ {
    val cl = this.getClass.getClassLoader

    val urls = cl match {
      case cl: java.net.URLClassLoader => cl.getURLs.toList
      case a =>
        sys.error("[SparkInterpreter] Unexpected class loader: " + a.getClass)
    }
    val classpath = urls map { _.toString }

    settings.classpath.value = classpath.distinct.mkString(java.io.File.pathSeparator)
  }

  private var sparkIMain: SparkIMain = null
  var context: SparkContext = _

  protected def createSparkContext(classServerUri: String): SparkContext = {
    val execUri = System.getenv("SPARK_EXECUTOR_URI")
    val conf = new SparkConf()
      // NOTE: Can easily test on a standalone cluster by running
      //       ./sbin/start-all.sh (master/worker configured for localhost
      //       by default) as long as have SSH service enabled
      //.setMaster("spark://Roberts-MacBook-Pro-3.local:7077")
      .setMaster("local[*]")
      .setAppName("Spark shell")
      //.setJars(jars)
      .set("spark.repl.class.uri", classServerUri)
    if (execUri != null) {
      conf.set("spark.executor.uri", execUri)
    }
    if (System.getenv("SPARK_HOME") != null) {
      conf.setSparkHome(System.getenv("SPARK_HOME"))
    }
    val sparkContext = new SparkContext(conf)
    sparkContext
  }

  def interpret(code: String) = {
    require(sparkIMain != null)

    sparkIMain.interpret(code)
  }

  // NOTE: Convention is to force parentheses if a side effect occurs.
  def start() = {
    require(sparkIMain == null)

    sparkIMain = new SparkIMain(settings, new JPrintWriter(Console.out, true))

    Console.out.println("Initializing interpreter")
    sparkIMain.initializeSynchronous()

    sparkIMain.beQuietDuring {
      Console.out.println("Creating a new context identified by 'sc'")
      this.context = createSparkContext(sparkIMain.classServer.uri)
      sparkIMain.bind(
        "sc", "org.apache.spark.SparkContext",
        this.context, List( """@transient"""))

      Console.out.println("Importing org.apache.spark.SparkContext._")
      sparkIMain.addImports("org.apache.spark.SparkContext._")
    }
    sparkIMain
  }

  // NOTE: Convention is to force parentheses if a side effect occurs.
  def stop() = {
    require(sparkIMain != null)

    Console.out.println("Shutting down interpreter")
    sparkIMain.beQuietDuring {
      sparkIMain.interpret("""sc.stop""")
    }

    sparkIMain.close

    sparkIMain = null
  }
}

