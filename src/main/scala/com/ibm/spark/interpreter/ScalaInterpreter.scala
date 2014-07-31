package com.ibm.spark.interpreter

import java.io.{PrintWriter, StringWriter}

import com.ibm.spark.utils.MultiWriter
import org.apache.spark.repl.{SparkCommandLine, SparkIMain}
import org.slf4j.LoggerFactory

import scala.tools.nsc.interpreter.OutputStream
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
  private val lastResultOut = new StringWriter()
  private val multiWriter =
    MultiWriter(List(new PrintWriter(out), lastResultOut))

  override def interpret(code: String, silent: Boolean = false) = {
    require(sparkIMain != null)

    var result: IR.Result = null
    var output: ExecutionOutput = ""

    if (silent) {
      sparkIMain.beSilentDuring {
        result = sparkIMain.interpret(code)
      }
    } else {
      result = sparkIMain.interpret(code)
      output = lastResultOut.toString
    }

    // Clear our output (per result)
    lastResultOut.getBuffer.setLength(0)

    (result, output)
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

    sparkIMain = new SparkIMain(settings, new JPrintWriter(multiWriter, true))

    logger.info("Initializing interpreter")
    sparkIMain.initializeSynchronous()

    sparkIMain.beQuietDuring {
      logger.info("Adding com.ibm.spark.interpreter.printers._ to imports")

      // TODO: Write printer overrides for Console and System.out and System.err
      //sparkIMain.addImports("com.ibm.spark.interpreter.printers._")

      // NOTE: This technically works, but only one wrapper is allowed and it
      //       feels less stable than shadowing the set of prints
      sparkIMain.setExecutionWrapper("Console.withOut(Console.err)")

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

