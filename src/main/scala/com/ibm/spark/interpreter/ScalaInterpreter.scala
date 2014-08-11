package com.ibm.spark.interpreter

import java.io.ByteArrayOutputStream
import java.net.{URLClassLoader, URL}
import java.nio.charset.Charset
import com.ibm.spark.interpreter.imports.printers.{WrapperConsole, WrapperSystem}
import com.ibm.spark.utils.{LogLike, MultiOutputStream}
import org.apache.spark.repl.{SparkCommandLine, SparkIMain}
import scala.tools.nsc.backend.{JavaPlatform, MSILPlatform}
import scala.tools.nsc._
import scala.tools.nsc.interpreter._
import scala.tools.nsc.reporters.Reporter
import scala.tools.nsc.util.{ClassPath, JavaClassPath, MergedClassPath}
import scala.tools.reflect.StdTags
import scala.tools.util.PathResolver

case class ScalaInterpreter(
  args: List[String],
  out: OutputStream
) extends Interpreter with LogLike {
  private val ExecutionExceptionName = "lastException"
  val settings: Settings = new SparkCommandLine(args).settings

  private val thisClassLoader = this.getClass.getClassLoader
  private val internalClassloader =
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
    settings.embeddedDefaults(internalClassloader)
  }

  private val lastResultOut = new ByteArrayOutputStream()
  private val multiOutputStream = MultiOutputStream(List(out, lastResultOut))
  private var sparkIMain: SparkIMain = _

  /**
   * Adds jars to the runtime and compile time classpaths. Does not work with
   * directories or expanding star in a path.
   * @param jars The list of jar locations
   */
  override def addJars(jars: URL*): Unit = {
    require(!sparkIMain.global.forMSIL) // Only support JavaPlatform

    //
    // Update runtime classpath
    //
    jars.foreach(internalClassloader.addJar)

    //
    // Update compile time classpath
    //
    val platform = sparkIMain.global.platform.asInstanceOf[JavaPlatform]

    // Collect our new jars and add them to the existing set of classpaths
    val allClassPaths = (
      platform.classPath
        .asInstanceOf[MergedClassPath[platform.BinaryRepr]].entries
      ++
      internalClassloader.getURLs.map(url =>
        platform.classPath.context.newClassPath(
          io.AbstractFile.getFile(url.getPath))
      )
    ).distinct

    // Combine all of our classpaths (old and new) into one merged classpath
    val newClassPath = new MergedClassPath(
      allClassPaths,
      platform.classPath.context
    )

    // TODO: Investigate better way to set this... one thought it to provide
    //       a classpath in the currentClassPath (which is merged) that can be
    //       replaced using updateClasspath, but would that work more than once?
    val fieldSetter = platform.getClass.getMethods
      .find(_.getName.endsWith("currentClassPath_$eq")).get
    fieldSetter.invoke(platform, Some(newClassPath))

    // Reload all jars specified into our compiler
    sparkIMain.global.invalidateClassPathEntries(jars.map(_.getPath): _*)
  }

  override def interpret(code: String, silent: Boolean = false):
    (IR.Result, Either[ExecuteOutput, ExecuteError]) =
  {
    require(sparkIMain != null)
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

    sparkIMain =
      new SparkIMain(settings, new JPrintWriter(multiOutputStream, true))

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

  def classServerURI() = {
    sparkIMain.classServer.uri
  }

  override def doQuietly[T](body: => T): T = {
    sparkIMain.beQuietDuring[T](body)
  }

  override def bind(variableName: String, typeName: String, value: Any, modifiers: List[String]): Unit = {
    sparkIMain.bind(variableName,typeName,value,modifiers)
  }
}

