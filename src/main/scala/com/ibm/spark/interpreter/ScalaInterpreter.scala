package com.ibm.spark.interpreter

import java.io.{ByteArrayOutputStream, PrintStream, PrintWriter, StringWriter}
import java.nio.charset.Charset

import com.ibm.spark.interpreter.imports.printers.{WrapperSystem, WrapperConsole}
import com.ibm.spark.utils.MultiOutputStream
import org.apache.commons.io.output.WriterOutputStream
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
  private val ExecutionExceptionName = "lastException"
  val settings: Settings = new SparkCommandLine(args).settings

  /* Add scala.runtime libraries to interpreter classpath */ {
    val cl = this.getClass.getClassLoader

    val urls = cl match {
      case cl: java.net.URLClassLoader => cl.getURLs.toList
      case a => // TODO: Should we really be using sys.error here?
        sys.error("[SparkInterpreter] Unexpected class loader: " + a.getClass)
    }
    val classpath = urls map { _.toString }

    settings.classpath.value =
      classpath.distinct.mkString(java.io.File.pathSeparator)
  }

  /**
   * TODO: Refactor this such that SparkIMain is not exposed publicly!
   */
  var sparkIMain: SparkIMain = _
  private val lastResultOut = new ByteArrayOutputStream()
  private val multiOutputStream = MultiOutputStream(List(out, lastResultOut))

  override def interpret(code: String, silent: Boolean = false):
    (IR.Result, Either[ExecuteOutput, ExecuteError]) =
  {
    require(sparkIMain != null)
    println(code)

    var result: IR.Result = null
    var output: ExecuteOutput = ""

    if (silent) {
      sparkIMain.beSilentDuring {
        result = sparkIMain.interpret(code)
      }
    } else {
      result = sparkIMain.interpret(code)
      output =
        lastResultOut.toString(Charset.defaultCharset().displayName()).trim
    }

    // Clear our output (per result)
    lastResultOut.reset()

    // Determine whether to provide an error or output
    result match {
      case IR.Success => (result, Left(output))
      case IR.Incomplete => (result, Left(output))
      case IR.Error =>
        val x = sparkIMain.valueOfTerm(ExecutionExceptionName)
        (result, Right(sparkIMain.valueOfTerm(ExecutionExceptionName) match {
          // Runtime error
          case Some(e) =>
            val ex = e.asInstanceOf[Throwable]
            ExecuteError(
              ex.getClass.getName,
              ex.getLocalizedMessage,
              ex.getStackTrace.map(_.toString).toList
            )
          // Compile time error, need to check internal reporter
          case _ =>
            if (sparkIMain.reporter.hasErrors)
              // TODO: This wrapper is not needed when just getting compile
              // error that we are not parsing... maybe have it be purely
              // output and have the error check this?
              ExecuteError(
                "Compile Error", output, List()
              )
            else
              ExecuteError("Unknown", "Unable to retrieve error!", List())
        }))
    }
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

    sparkIMain = new SparkIMain(
      settings,
      new JPrintWriter(multiOutputStream, true)
    )

    logger.info("Initializing interpreter")
    sparkIMain.initializeSynchronous()

    sparkIMain.beQuietDuring {
      logger.info("Rerouting Console and System related input and output")

      // Mask the Console and System objects with our wrapper implementations
      // and dump the Console methods into the public namespace (similar to
      // the Predef approach)
      sparkIMain.bind("Console",
        new WrapperConsole(System.in, multiOutputStream, multiOutputStream))
      sparkIMain.bind("System",
        new WrapperSystem(System.in, multiOutputStream, multiOutputStream))
      sparkIMain.addImports("Console._")

      // TODO: Investigate using execution wrapper to catch errors

      logger.info("Adding org.apache.spark.SparkContext._ to imports")
      sparkIMain.addImports("org.apache.spark.SparkContext._")
    }

    this
  }

  // NOTE: Convention is to force parentheses if a side effect occurs.
  override def stop() = {
    logger.info("Shutting down interpreter")

    if (sparkIMain != null) sparkIMain.close()

    sparkIMain = null

    this
  }
}

