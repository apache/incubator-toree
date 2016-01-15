/*
 * Copyright 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.toree.kernel.interpreter.scala

import java.io.{BufferedReader, ByteArrayOutputStream, InputStreamReader, PrintStream}
import java.net.{URL, URLClassLoader}
import java.nio.charset.Charset
import java.util.concurrent.ExecutionException

import akka.actor.Actor
import akka.actor.Actor.Receive
import org.apache.toree.global.StreamState
import org.apache.toree.interpreter
import org.apache.toree.interpreter._
import org.apache.toree.interpreter.imports.printers.{WrapperConsole, WrapperSystem}
import org.apache.toree.kernel.api.{KernelLike, KernelOptions}
import org.apache.toree.utils.{MultiOutputStream, TaskManager}
import org.apache.spark.SparkContext
import org.apache.spark.repl.{SparkCommandLine, SparkIMain, SparkJLineCompletion}
import org.apache.spark.sql.SQLContext
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.concurrent.{Await, Future}
import scala.language.reflectiveCalls
import scala.tools.nsc.backend.JavaPlatform
import scala.tools.nsc.interpreter.{OutputStream, IR, JPrintWriter, InputStream}
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.util.{ClassPath, MergedClassPath}
import scala.tools.nsc.{Global, Settings, io}
import scala.util.{Try => UtilTry}

class ScalaInterpreter() extends Interpreter {

  protected val logger = LoggerFactory.getLogger(this.getClass.getName)

  private val ExecutionExceptionName = "lastException"
  protected var settings: Settings = null

  protected val _thisClassloader = this.getClass.getClassLoader
  protected val _runtimeClassloader =
    new URLClassLoader(Array(), _thisClassloader) {
      def addJar(url: URL) = this.addURL(url)
    }


  protected val lastResultOut = new ByteArrayOutputStream()
  protected val multiOutputStream = MultiOutputStream(List(Console.out, lastResultOut))
  private var taskManager: TaskManager = _
  var sparkIMain: SparkIMain = _
  protected var jLineCompleter: SparkJLineCompletion = _

  protected def newSparkIMain(
    settings: Settings, out: JPrintWriter
  ): SparkIMain = {
    val s = new SparkIMain(settings, out)
    s.initializeSynchronous()

    s
  }

  private var maxInterpreterThreads:Int = TaskManager.DefaultMaximumWorkers

  protected def newTaskManager(): TaskManager =
    new TaskManager(maximumWorkers = maxInterpreterThreads)

  protected def newSettings(args: List[String]): Settings =
    new SparkCommandLine(args).settings

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

    // Refresh all of our variables
    refreshDefinitions()
  }

  // TODO: Need to figure out a better way to compare the representation of
  //       an annotation (contained in AnnotationInfo) with various annotations
  //       like scala.transient
  protected def convertAnnotationsToModifiers(
    annotationInfos: List[Global#AnnotationInfo]
  ) = annotationInfos map {
    case a if a.toString == "transient" => "@transient"
    case a =>
      logger.debug(s"Ignoring unknown annotation: $a")
      ""
  } filterNot {
    _.isEmpty
  }

  protected def convertScopeToModifiers(scopeSymbol: Global#Symbol) = {
    (if (scopeSymbol.isImplicit) "implicit" else "") ::
      Nil
  }

  protected def buildModifierList(termNameString: String) = {
    import scala.language.existentials
    val termSymbol = sparkIMain.symbolOfTerm(termNameString)


    convertAnnotationsToModifiers(
      if (termSymbol.hasAccessorFlag) termSymbol.accessed.annotations
      else termSymbol.annotations
    ) ++ convertScopeToModifiers(termSymbol)
  }

  protected def refreshDefinitions(): Unit = {
    sparkIMain.definedTerms.foreach(termName => {
      val termNameString = termName.toString
      val termTypeString = sparkIMain.typeOfTerm(termNameString).toLongString
      sparkIMain.valueOfTerm(termNameString) match {
        case Some(termValue)  =>
          val modifiers = buildModifierList(termNameString)
          logger.debug(s"Rebinding of $termNameString as " +
            s"${modifiers.mkString(" ")} $termTypeString")
          UtilTry(sparkIMain.beSilentDuring {
            sparkIMain.bind(
              termNameString, termTypeString, termValue, modifiers
            )
          })
        case None             =>
          logger.debug(s"Ignoring rebinding of $termNameString")
      }
    })
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
    logger.debug(s"newClassPath: ${newClassPath}")

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

  override def init(kernel: KernelLike): Interpreter = {
    val args = interpreterArgs(kernel)
    this.settings = newSettings(args)

    this.settings.classpath.value = buildClasspath(_thisClassloader)
    this.settings.embeddedDefaults(_runtimeClassloader)

    maxInterpreterThreads = maxInterpreterThreads(kernel)

    start()
    bindKernelVarialble(kernel)

    this
  }

  protected[scala] def buildClasspath(classLoader: ClassLoader): String = {

    def toClassLoaderList( classLoader: ClassLoader ): Seq[ClassLoader] = {
      @tailrec
      def toClassLoaderListHelper( aClassLoader: ClassLoader, theList: Seq[ClassLoader]):Seq[ClassLoader] = {
        if( aClassLoader == null )
          return theList

        toClassLoaderListHelper( aClassLoader.getParent, aClassLoader +: theList )
      }
      toClassLoaderListHelper(classLoader, Seq())
    }

    val urls = toClassLoaderList(classLoader).flatMap{
        case cl: java.net.URLClassLoader => cl.getURLs.toList
        case a => List()
    }

    urls.foldLeft("")((l, r) => ClassPath.join(l, r.toString))
  }

  protected def interpreterArgs(kernel: KernelLike): List[String] = {
    import scala.collection.JavaConverters._
    kernel.config.getStringList("interpreter_args").asScala.toList
  }

  protected def maxInterpreterThreads(kernel: KernelLike): Int = {
    kernel.config.getInt("max_interpreter_threads")
  }

  protected def bindKernelVarialble(kernel: KernelLike): Unit = {
    doQuietly {
      bind(
        "kernel", "org.apache.toree.kernel.api.Kernel",
        kernel, List( """@transient implicit""")
      )
    }
  }

  override def interrupt(): Interpreter = {
    require(sparkIMain != null && taskManager != null)

    // Force dumping of current task (begin processing new tasks)
    taskManager.restart()

    this
  }

  override def interpret(code: String, silent: Boolean = false):
  (Results.Result, Either[ExecuteOutput, ExecuteFailure]) = {
    val starting = (Results.Success, Left(""))
    interpretRec(code.trim.split("\n").toList, false, starting)
  }

  def truncateResult(result:String, showType:Boolean =false, noTruncate: Boolean = false): String = {
    val resultRX="""(?s)(res\d+):\s+(.+)\s+=\s+(.*)""".r

    result match {
      case resultRX(varName,varType,resString) => {
          var returnStr=resString
          if (noTruncate)
          {
            val r=read(varName)
            returnStr=r.getOrElse("").toString
          }

          if (showType)
            returnStr=varType+" = "+returnStr

        returnStr

      }
      case _ => ""
    }


  }

  protected def interpretRec(lines: List[String], silent: Boolean = false, results: (Results.Result, Either[ExecuteOutput, ExecuteFailure])): (Results.Result, Either[ExecuteOutput, ExecuteFailure]) = {
    lines match {
      case Nil => results
      case x :: xs =>
        val output = interpretLine(x)

        output._1 match {
          // if success, keep interpreting and aggregate ExecuteOutputs
          case Results.Success =>
            val result = for {
              originalResult <- output._2.left
            } yield(truncateResult(originalResult, KernelOptions.showTypes,KernelOptions.noTruncation))
            interpretRec(xs, silent, (output._1, result))

          // if incomplete, keep combining incomplete statements
          case Results.Incomplete =>
            xs match {
              case Nil => interpretRec(Nil, silent, (Results.Incomplete, results._2))
              case _ => interpretRec(x + "\n" + xs.head :: xs.tail, silent, results)
            }

          //
          case Results.Aborted =>
            output
             //interpretRec(Nil, silent, output)

          // if failure, stop interpreting and return the error
          case Results.Error =>
            val result = for {
              curr <- output._2.right
            } yield curr
            interpretRec(Nil, silent, (output._1, result))
        }
    }
  }


  protected def interpretLine(line: String, silent: Boolean = false):
    (Results.Result, Either[ExecuteOutput, ExecuteFailure]) =
  {
    require(sparkIMain != null && taskManager != null)
    logger.trace(s"Interpreting line: $line")

    val futureResult = interpretAddTask(line, silent)

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

  protected def interpretAddTask(code: String, silent: Boolean) = {
    taskManager.add {
      // Add a task using the given state of our streams
      StreamState.withStreams {
        if (silent) {
          sparkIMain.beSilentDuring {
            sparkIMain.interpret(code)
          }
        } else {
          sparkIMain.interpret(code)
        }
      }
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
        sparkIMain.directBind(
          ExecutionExceptionName,
          classOf[Throwable].getName,
          null
        )
        ExecuteError(
          ex.getClass.getName,
          ex.getLocalizedMessage,
          ex.getStackTrace.map(_.toString).toList
        )
      // Compile time error, need to check internal reporter
      case _ =>
        if (sparkIMain.isReportingErrors)
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

    logger.debug("Initializing task manager")
    taskManager.start()

    sparkIMain =
      newSparkIMain(settings, new JPrintWriter(multiOutputStream, true))


    //logger.debug("Initializing interpreter")
    //sparkIMain.initializeSynchronous()

    logger.debug("Initializing completer")
    jLineCompleter = new SparkJLineCompletion(sparkIMain)

    sparkIMain.beQuietDuring {
      //logger.info("Rerouting Console and System related input and output")
      //updatePrintStreams(System.in, multiOutputStream, multiOutputStream)

//   ADD IMPORTS generates too many classes, client is responsible for adding import
      logger.debug("Adding org.apache.spark.SparkContext._ to imports")
      sparkIMain.addImports("org.apache.spark.SparkContext._")
    }

    this
  }

  override def updatePrintStreams(
    in: InputStream, out: OutputStream, err: OutputStream
  ): Unit = {
    val inReader = new BufferedReader(new InputStreamReader(in))
    val outPrinter = new PrintStream(out)
    val errPrinter = new PrintStream(err)

    sparkIMain.beQuietDuring {
      sparkIMain.bind(
        "Console", classOf[WrapperConsole].getName,
        new WrapperConsole(inReader, outPrinter, errPrinter),
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
    sparkIMain.classServerUri
  }

  override def doQuietly[T](body: => T): T = {
    require(sparkIMain != null)
    sparkIMain.beQuietDuring[T](body)
  }

  override def bindSparkContext(sparkContext: SparkContext) = {
    val bindName = "sc"

    doQuietly {
      logger.debug(s"Binding SparkContext into interpreter as $bindName")
      bind(
        bindName,
        "org.apache.spark.SparkContext",
        sparkContext,
        List( """@transient""")
      )

      // NOTE: This is needed because interpreter blows up after adding
      //       dependencies to SparkContext and Interpreter before the
      //       cluster has been used... not exactly sure why this is the case
      // TODO: Investigate why the cluster has to be initialized in the kernel
      //       to avoid the kernel's interpreter blowing up (must be done
      //       inside the interpreter)
      logger.debug("Initializing Spark cluster in interpreter")

      doQuietly {
        interpret(Seq(
          "val $toBeNulled = {",
          "  var $toBeNulled = sc.emptyRDD.collect()",
          "  $toBeNulled = null",
          "}"
        ).mkString("\n").trim())
      }
    }
  }

  override def bindSqlContext(sqlContext: SQLContext): Unit = {
    val bindName = "sqlContext"

    doQuietly {
      // TODO: This only adds the context to the main interpreter AND
      //       is limited to the Scala interpreter interface
      logger.debug(s"Binding SQLContext into interpreter as $bindName")
      bind(
        bindName,
        classOf[SQLContext].getName,
        sqlContext,
        List( """@transient""")
      )

      sqlContext
    }
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

  override def lastExecutionVariableName: Option[String] = {
    require(sparkIMain != null)

    // TODO: Get this API method changed back to public in Apache Spark
    val lastRequestMethod = classOf[SparkIMain].getDeclaredMethod("lastRequest")
    lastRequestMethod.setAccessible(true)

    val request =
      lastRequestMethod.invoke(sparkIMain).asInstanceOf[SparkIMain#Request]

    val mostRecentVariableName = sparkIMain.mostRecentVar

    request.definedNames.map(_.toString).find(_ == mostRecentVariableName)
  }

  override def completion(code: String, pos: Int): (Int, List[String]) = {
    require(jLineCompleter != null)

    logger.debug(s"Attempting code completion for ${code}")
    val regex = """[0-9a-zA-Z._]+$""".r
    val parsedCode = (regex findAllIn code).mkString("")

    logger.debug(s"Attempting code completion for ${parsedCode}")
    val result = jLineCompleter.completer().complete(parsedCode, pos)

    (result.cursor, result.candidates)
  }

  override def classLoader: ClassLoader = _runtimeClassloader
}

