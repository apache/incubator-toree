package com.ibm.spark.interpreter

import java.io.ByteArrayOutputStream
import java.net.{URL, URLClassLoader}
import java.nio.charset.Charset

import com.ibm.spark.interpreter.imports.printers.{WrapperConsole, WrapperSystem}
import com.ibm.spark.utils.{TaskManager, LogLike, MultiOutputStream}
import org.apache.spark.repl.{SparkCommandLine, SparkIMain}

import scala.tools.nsc._
import scala.tools.nsc.backend.JavaPlatform
import scala.tools.nsc.interpreter._
import scala.tools.nsc.io._
import scala.tools.nsc.util.MergedClassPath

import scala.language.reflectiveCalls

case class ScalaInterpreter(
  args: List[String],
  out: OutputStream
) extends Interpreter with LogLike {
  private val ExecutionExceptionName = "lastException"
  val settings: Settings = new SparkCommandLine(args).settings

  private val thisClassLoader = this.getClass.getClassLoader
  private val runtimeClassloader =
    new URLClassLoader(Array(), thisClassLoader) {
      def addJar(url: URL) = this.addURL(url)
    }

  /* Add scala.runtime libraries to interpreter classpath */ {
    val urls = thisClassLoader match {
      case cl: java.net.URLClassLoader => cl.getURLs.toList
      case a => // TODO: Should we really be using sys.error here?
        sys.error("[SparkInterpreter] Unexpected class loader: " + a.getClass)
    }
    val classpath = urls.map(_.toString)

    settings.classpath.value =
      classpath.distinct.mkString(java.io.File.pathSeparator)
    settings.embeddedDefaults(runtimeClassloader)
  }

  private val lastResultOut = new ByteArrayOutputStream()
  private val multiOutputStream = MultiOutputStream(List(out, lastResultOut))
  private var taskManager: TaskManager = _
  protected var sparkIMain: SparkIMain = _

  /**
   * Adds jars to the runtime and compile time classpaths. Does not work with
   * directories or expanding star in a path.
   * @param jars The list of jar locations
   */
  override def addJars(jars: URL*): Unit = {
    // Enable Scala class support
    val global = sparkIMain.global
    import global._
    new Run // Initializes something needed for Scala classes

    jars.foreach(runtimeClassloader.addJar)
    updateCompilerClassPath(jars : _*)
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
    (IR.Result, Either[ExecuteOutput, ExecuteError]) =
  {
    require(sparkIMain != null && taskManager != null)
    logger.debug(s"Interpreting code: $code")

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
  // sparkKernelContext.toSparkContext <-- you lock the config
  //
  // sparkKernelContext
  // Interpreter(sparkKernelContext)
  //     sparkKernelContext.config.set(...) <-- update the uri
  //     bind("sc", sparkKernelContext.toSparkContext)
  //
  // SparkScalaInterpreter.start.with( "sc", {} )
  override def start() = {
    require(sparkIMain == null && taskManager == null)

    taskManager = new TaskManager

    logger.info("Initializing task manager")
    taskManager.start()

    sparkIMain =
      new SparkIMain(settings, new JPrintWriter(multiOutputStream, true))

    logger.info("Initializing interpreter")
    sparkIMain.initializeSynchronous()

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
      sparkIMain.bind("Console", new WrapperConsole(in, out, err))
      sparkIMain.bind("System", new WrapperSystem(in, out, err))
      sparkIMain.addImports("Console._")
    }
  }

  // NOTE: Convention is to force parentheses if a side effect occurs.
  override def stop() = {
    logger.info("Shutting down interpreter")

    // Shut down the task manager (kills current execution
    if (taskManager != null) taskManager.stop()
    taskManager = null

    // Close the entire interpreter (loses all state)
    if (sparkIMain != null) sparkIMain.close()
    sparkIMain = null

    this
  }

  def classServerURI = sparkIMain.classServer.uri

  override def doQuietly[T](body: => T): T = {
    sparkIMain.beQuietDuring[T](body)
  }

  override def bind(
    variableName: String, typeName: String,
    value: Any, modifiers: List[String]
  ): Unit = {
    sparkIMain.bind(variableName,typeName,value,modifiers)
  }
}

