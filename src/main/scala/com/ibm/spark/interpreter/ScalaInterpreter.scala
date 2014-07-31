package com.ibm.spark.interpreter

import org.apache.spark.repl.{SparkCommandLine, SparkIMain}
import org.slf4j.LoggerFactory

import scala.tools.nsc.interpreter._
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.JPrintWriter

case class ScalaInterpreter(
  args: List[String],
  out: OutputStream
) extends Interpreter {
  private val logger = LoggerFactory.getLogger(classOf[ScalaInterpreter])
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

  /**
   * TODO: Refactor this such that SparkIMain is not exposed publicly!
   */
  var sparkIMain: SparkIMain = _

  override def interpret(code: String, silent: Boolean = false) = {
    require(sparkIMain != null)

    if (silent) sparkIMain.beSilentDuring {
      sparkIMain.interpret(code)
    } else sparkIMain.interpret(code)
  }

  // NOTE: Convention is to force parentheses if a side effect occurs.
  // val int = new SSI with SparkSupport;
  // val conf = new SparkConf()
  // int.withSparkConfig(conf).bind( "sc", conf => new SparkContext(conf) )
  //
  // int.withContext( "sc", conf, conf => new SparkContext(conf) )
  //
  // val sparkKernelContext = new (...)
  // sparkKernelContext.toSparkConext <-- you lock the config
  //
  // sparkKernelContext
  // Interperter(sparkKernelContext)
  //     sparkKernelContext.config.set(...) <-- update the uri
  //     bind("sc", sparkKernelContext.toSparkContext)
  //
  // SparkScalaInterpreter.start.with( "sc", {} )
  override def start() = {
    require(sparkIMain == null)

    sparkIMain = new SparkIMain(settings, new JPrintWriter(out, true))

    logger.info("Initializing interpreter")
    sparkIMain.initializeSynchronous()

    sparkIMain.beQuietDuring {
      logger.info("Adding org.apache.spark.SparkContext._ to imports")
      sparkIMain.addImports("org.apache.spark.SparkContext._")
    }

    this
  }

  // NOTE: Convention is to force parentheses if a side effect occurs.
  override def stop() = {
    require(sparkIMain != null)

    logger.info("Shutting down interpreter")
    /*sparkIMain.beQuietDuring {
      context.stop()
      //sparkIMain.interpret("""sc.stop""")
    }*/

    sparkIMain.close()

    sparkIMain = null

    this
  }
}

