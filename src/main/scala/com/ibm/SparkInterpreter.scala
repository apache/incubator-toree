package com.ibm

import org.apache.spark.repl.{SparkCommandLine, SparkIMain}
import org.apache.spark.{SparkConf, SparkContext}

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.{JPrintWriter, LoopCommands}

// TODO: Convert our Console.out... to a standard provided to this class
class SparkInterpreter(args: List[String]) {
  private val SparkApplicationName = "IBM Spark Kernel"

  val settings: Settings = new SparkCommandLine(args).settings

  /* Add scala.runtime libraries to interpreter classpath */ {
    val cl = this.getClass.getClassLoader

    val urls = cl match {
      case cl: java.net.URLClassLoader => cl.getURLs.toList
      case a => // TODO: Should we really be using sys.error here?
        sys.error("[SparkInterpreter] Unexpected class loader: " + a.getClass)
    }
    val classpath = urls map { _.toString }

    settings.classpath.value = classpath.distinct.mkString(java.io.File.pathSeparator)
  }

  private var sparkIMain: SparkIMain = _
  var context: SparkContext = _

  // TODO: Move variable logic to separate traits
  protected def createSparkContext(classServerUri: String): SparkContext = {
    val execUri = System.getenv("SPARK_EXECUTOR_URI")
    val conf = new SparkConf()
      // NOTE: Can easily test on a standalone cluster by running
      //       ./sbin/start-all.sh (master/worker configured for localhost
      //       by default) as long as have SSH service enabled
      .setMaster("spark://Roberts-MacBook-Pro-3.local:7077")
      // NOTE: The above as well as local[*] have worked with sbt run after adding
      //       fork = true and removing akka libraries
      //.setMaster("local[*]")
      .setAppName(SparkApplicationName)
      //.setJars(jars)
      .set("spark.repl.class.uri", classServerUri)
      //.set("spark.driver.host", "localhost")
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

      Console.out.println("Adding org.apache.spark.SparkContext._ to imports")
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

    sparkIMain.close()

    sparkIMain = null
  }
}

