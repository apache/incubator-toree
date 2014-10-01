package com.ibm.spark.interpreter

import java.io.ByteArrayOutputStream
import java.net.{URL, URLClassLoader}
import java.nio.charset.Charset
import java.util.concurrent.ExecutionException

import com.ibm.spark.interpreter.imports.printers.{WrapperConsole, WrapperSystem}
import com.ibm.spark.utils.{TaskManager, LogLike, MultiOutputStream}
import org.apache.spark.repl.{SparkCommandLine, SparkIMain}
import com.ibm.spark.utils.{LogLike, MultiOutputStream}
import org.apache.spark.repl.{SparkJLineCompletion, SparkCommandLine, SparkIMain}
import scala.concurrent.{Await, Future}
import scala.tools.nsc._
import scala.tools.nsc.backend.JavaPlatform
import scala.tools.nsc.interpreter._
import scala.tools.nsc.io._
import scala.tools.nsc.util.MergedClassPath

import scala.language.reflectiveCalls

class ScalaInterpreter(
  args: List[String],
  out: OutputStream
) extends Interpreter with LogLike {
  this: SparkIMainProducerLike
    with TaskManagerProducerLike
    with SettingsProducerLike =>

  private val ExecutionExceptionName = "lastException"
  val settings: Settings = newSettings(args)

  private val _thisClassloader = this.getClass.getClassLoader
  protected val _runtimeClassloader =
    new URLClassLoader(Array(), _thisClassloader) {
      def addJar(url: URL) = this.addURL(url)
    }

  /* Add scala.runtime libraries to interpreter classpath */ {
    val urls = _thisClassloader match {
      case cl: java.net.URLClassLoader => cl.getURLs.toList
      case a => // TODO: Should we really be using sys.error here?
        sys.error("[SparkInterpreter] Unexpected class loader: " + a.getClass)
    }
    val classpath = urls.map(_.toString)

    settings.classpath.value =
      classpath.distinct.mkString(java.io.File.pathSeparator)
    settings.embeddedDefaults(_runtimeClassloader)
  }

  private val lastResultOut = new ByteArrayOutputStream()
  private val multiOutputStream = MultiOutputStream(List(out, lastResultOut))
  private var taskManager: TaskManager = _
  protected var sparkIMain: SparkIMain = _
  protected var jLineCompleter: SparkJLineCompletion = _

  /**
   * Adds jars to the runtime and compile time classpaths. Does not work with
   * directories or expanding star in a path.
   * @param jars The list of jar locations
   */
  override def addJars(jars: URL*): Unit = {
    // Enable Scala class support
    reinitializeSymbols()

    jars.foreach(_runtimeClassloader.addJar)
    updateCompilerClassPath(jars : _*)
  }

  protected def reinitializeSymbols(): Unit = {
    val global = sparkIMain.global
    import global._
    new Run // Initializes something needed for Scala classes
  }

  protected def updateCompilerClassPath( jars: URL*): Unit = {
    require(!sparkIMain.global.forMSIL) // Only support JavaPlatform

    val platform = sparkIMain.global.platform.asInstanceOf[JavaPlatform]

    val newClassPath = mergeJarsIntoClassPath(platform, jars:_*)

    // TODO: Investigate better way to set this... one thought is to provide
    //       a classpath in the currentClassPath (which is merged) that can be
    //       replaced using updateClasspath, but would that work more than once?
    val fieldSetter = platform.getClass.getMethods
      .find(_.getName.endsWith("currentClassPath_$eq")).get
    fieldSetter.invoke(platform, Some(newClassPath))

    // Reload all jars specified into our compiler
    sparkIMain.global.invalidateClassPathEntries(jars.map(_.getPath): _*)
  }

  protected def mergeJarsIntoClassPath(platform: JavaPlatform, jars: URL*): MergedClassPath[AbstractFile] = {
    // Collect our new jars and add them to the existing set of classpaths
    val allClassPaths = (
      platform.classPath
        .asInstanceOf[MergedClassPath[AbstractFile]].entries
        ++
        jars.map(url =>
          platform.classPath.context.newClassPath(
            io.AbstractFile.getFile(url.getPath))
        )
      ).distinct

    // Combine all of our classpaths (old and new) into one merged classpath
    new MergedClassPath(
      allClassPaths,
      platform.classPath.context
    )
  }

  override def interrupt(): Interpreter = {
    require(sparkIMain != null && taskManager != null)

    // Force dumping of current task (begin processing new tasks)
    taskManager.restart()

    this
  }

  override def interpret(code: String, silent: Boolean = false):
    (Results.Result, Either[ExecuteOutput, ExecuteFailure]) =
  {
    require(sparkIMain != null && taskManager != null)
    logger.debug(s"Interpreting code: $code")

    val futureResult = interpretAddTask(code, silent)

    // Map the old result types to our new types
    val mappedFutureResult = interpretMapToCustomResult(futureResult)

    // Determine whether to provide an error or output
    val futureResultAndOutput = interpretMapToResultAndOutput(mappedFutureResult)

    val futureResultAndExecuteInfo =
      interpretMapToResultAndExecuteInfo(futureResultAndOutput)

    // Block indefinitely until our result has arrived
    import scala.concurrent.duration._
    Await.result(futureResultAndExecuteInfo, Duration.Inf)
  }

  protected def interpretAddTask(code: String, silent: Boolean) =
    taskManager.add {
      if (silent) {
        sparkIMain.beSilentDuring {
          sparkIMain.interpret(code)
        }
      } else {
        sparkIMain.interpret(code)
      }
    }

  protected def interpretMapToCustomResult(future: Future[IR.Result]) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    future map {
      case IR.Success             => Results.Success
      case IR.Error               => Results.Error
      case IR.Incomplete          => Results.Incomplete
    } recover {
      case ex: ExecutionException => Results.Aborted
    }
  }

  protected def interpretMapToResultAndOutput(future: Future[Results.Result]) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    future map {
      result =>
        val output =
          lastResultOut.toString(Charset.forName("UTF-8").name()).trim
        lastResultOut.reset()
        (result, output)
    }
  }

  protected def interpretMapToResultAndExecuteInfo(
    future: Future[(Results.Result, String)]
  ): Future[(Results.Result, Either[ExecuteOutput, ExecuteFailure])] =
  {
    import scala.concurrent.ExecutionContext.Implicits.global
    future map {
      case (Results.Success, output)    => (Results.Success, Left(output))
      case (Results.Incomplete, output) => (Results.Incomplete, Left(output))
      case (Results.Aborted, output)    => (Results.Aborted, Right(null))
      case (Results.Error, output)      =>
        val x = sparkIMain.valueOfTerm(ExecutionExceptionName)
        (
          Results.Error,
          Right(
            interpretConstructExecuteError(
              sparkIMain.valueOfTerm(ExecutionExceptionName),
              output
            )
          )
        )
    }
  }

  protected def interpretConstructExecuteError(value: Option[AnyRef], output: String) =
    value match {
      // Runtime error
      case Some(e) if e != null =>
        val ex = e.asInstanceOf[Throwable]
        // Clear runtime error message
        sparkIMain.directBind[Throwable](ExecutionExceptionName, null)
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
    }


  override def start() = {
    require(sparkIMain == null && taskManager == null)

    taskManager = newTaskManager()

    logger.info("Initializing task manager")
    taskManager.start()

    sparkIMain =
      newSparkIMain(settings, new JPrintWriter(multiOutputStream, true))

    logger.info("Initializing interpreter")
    sparkIMain.initializeSynchronous()

    logger.info("Initializing completer")
    jLineCompleter = new SparkJLineCompletion(sparkIMain)

    sparkIMain.beQuietDuring {
      logger.info("Rerouting Console and System related input and output")
      updatePrintStreams(System.in, multiOutputStream, multiOutputStream)

      // TODO: Investigate using execution wrapper to catch errors

      logger.info("Adding org.apache.spark.SparkContext._ to imports")
      sparkIMain.addImports("org.apache.spark.SparkContext._")
    }

    this
  }

  override def updatePrintStreams(
    in: InputStream, out: OutputStream, err: OutputStream
  ): Unit = {
    sparkIMain.beQuietDuring {
      sparkIMain.bind(
        "Console", classOf[WrapperConsole].getName,
        new WrapperConsole(in, out, err),
        List("""@transient""")
      )
      sparkIMain.bind(
        "System", classOf[WrapperSystem].getName,
        new WrapperSystem(in, out, err),
        List("""@transient""")
      )
      sparkIMain.addImports("Console._")
    }
  }

  // NOTE: Convention is to force parentheses if a side effect occurs.
  override def stop() = {
    logger.info("Shutting down interpreter")

    // Shut down the task manager (kills current execution
    if (taskManager != null) taskManager.stop()
    taskManager = null

    // Erase our completer
    jLineCompleter = null

    // Close the entire interpreter (loses all state)
    if (sparkIMain != null) sparkIMain.close()
    sparkIMain = null

    this
  }

  def classServerURI = {
    require(sparkIMain != null)
    sparkIMain.classServer.uri
  }

  override def doQuietly[T](body: => T): T = {
    require(sparkIMain != null)
    sparkIMain.beQuietDuring[T](body)
  }

  override def bind(
    variableName: String, typeName: String,
    value: Any, modifiers: List[String]
  ): Unit = {
    require(sparkIMain != null)
    sparkIMain.bind(variableName, typeName, value, modifiers)
  }

  override def read(variableName: String): Option[AnyRef] = {
    require(sparkIMain != null)
    val variable = sparkIMain.valueOfTerm(variableName)
    if (variable == null || variable.isEmpty) None
    else variable
  }

  override def completion(code: String, pos: Int): (Int, List[String]) = {
    require(jLineCompleter != null)

    logger.debug(s"Attempting code completion for ${code}")
    val regex = """[0-9a-zA-Z._]+$""".r
    val parsedCode = (regex findAllIn code).mkString("")

    logger.debug(s"Attempting code completion for ${parsedCode}")
    val result = jLineCompleter.completer().complete(parsedCode, pos)

    return (result.cursor, result.candidates)
  }
}

