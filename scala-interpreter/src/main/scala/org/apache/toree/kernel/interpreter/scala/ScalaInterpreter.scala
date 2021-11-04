/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package org.apache.toree.kernel.interpreter.scala

import java.io.ByteArrayOutputStream
import java.util.concurrent.{ExecutionException, TimeoutException, TimeUnit}
import com.typesafe.config.{Config, ConfigFactory}
import jupyter.Displayers
import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.repl.Main
import org.apache.toree.interpreter._
import org.apache.toree.kernel.api.{KernelLike, KernelOptions}
import org.apache.toree.utils.TaskManager
import org.slf4j.LoggerFactory
import org.apache.toree.kernel.BuildInfo
import org.apache.toree.kernel.protocol.v5.MIMEType
import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.concurrent.{Await, Future}
import scala.language.reflectiveCalls
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.{IR, OutputStream}
import scala.tools.nsc.util.ClassPath
import scala.util.matching.Regex
import scala.concurrent.duration.Duration

class ScalaInterpreter(private val config:Config = ConfigFactory.load) extends Interpreter with ScalaInterpreterSpecific {
  import ScalaInterpreter._

  ScalaDisplayers.ensureLoaded()

  protected var _kernel: KernelLike = _

  protected def kernel: KernelLike = _kernel

  protected val logger = LoggerFactory.getLogger(this.getClass.getName)

  protected val _thisClassloader = this.getClass.getClassLoader

  protected val lastResultOut = new ByteArrayOutputStream()

  private[scala] var taskManager: TaskManager = _

  /** Since the ScalaInterpreter can be started without a kernel, we need to ensure that we can compile things.
      Adding in the default classpaths as needed.
    */
  def appendClassPath(settings: Settings): Settings = {
    settings.classpath.value = buildClasspath(_thisClassloader)
    settings.embeddedDefaults(_runtimeClassloader)
    settings
  }

  protected var settings: Settings = newSettings(List())
  settings = appendClassPath(settings)


  private val maxInterpreterThreads: Int = {
     if(config.hasPath("max_interpreter_threads"))
       config.getInt("max_interpreter_threads")
     else
       TaskManager.DefaultMaximumWorkers
   }

   protected def newTaskManager(): TaskManager =
     new TaskManager(maximumWorkers = maxInterpreterThreads)

  /**
    * This has to be called first to initialize all the settings.
    *
    * @return The newly initialized interpreter
    */
   override def init(kernel: KernelLike): Interpreter = {
     this._kernel = kernel
     val args = interpreterArgs(kernel)
     settings = newSettings(args)
     settings = appendClassPath(settings)

     // https://issues.apache.org/jira/browse/TOREE-132?focusedCommentId=15104495&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-15104495
     settings.usejavacp.value = true

     start()

     // ensure bindings are defined before allowing user code to run
     bindVariables()

     this
   }

  protected def bindVariables(): Unit = {
    bindKernelVariable(kernel)
    bindSparkSession()
    bindSparkContext()
    defineImplicits()
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
     if (kernel == null || kernel.config == null) {
       List()
     }
     else {
       kernel.config.getStringList("interpreter_args").asScala.toList
     }
   }

   protected def maxInterpreterThreads(kernel: KernelLike): Int = {
     kernel.config.getInt("max_interpreter_threads")
   }

   protected def bindKernelVariable(kernel: KernelLike): Unit = {
     logger.warn(s"kernel variable: ${kernel}")
//     InterpreterHelper.kernelLike = kernel
//     interpret("import org.apache.toree.kernel.interpreter.scala.InterpreterHelper")
//     interpret("import org.apache.toree.kernel.api.Kernel")
//
//     interpret(s"val kernel = InterpreterHelper.kernelLike.asInstanceOf[org.apache.toree.kernel.api.Kernel]")

     doQuietly {

       bind(
         "kernel", "org.apache.toree.kernel.api.Kernel",
         kernel, List( """@transient implicit""")
       )
     }
   }

   override def interrupt(): Interpreter = {
     require(taskManager != null)

     // TODO: use SparkContext.setJobGroup to avoid killing all jobs
     kernel.sparkContext.cancelAllJobs()

     // give the task 100ms to complete before restarting the task manager
     import scala.concurrent.ExecutionContext.Implicits.global
     val finishedFuture = Future {
       while (taskManager.isExecutingTask) {
         Thread.sleep(10)
       }
     }

     try {
       Await.result(finishedFuture, Duration(100, TimeUnit.MILLISECONDS))
       // Await returned, no need to interrupt tasks.
     } catch {
       case timeout: TimeoutException =>
         // Force dumping of current task (begin processing new tasks)
         taskManager.restart()
     }

     this
   }

   override def interpret(code: String, silent: Boolean = false, output: Option[OutputStream]):
    (Results.Result, Either[ExecuteOutput, ExecuteFailure]) = {
     interpretBlock(code, silent)
   }

  def prepareResult(interpreterOutput: String,
                    showType: Boolean = KernelOptions.showTypes, // false
                    noTruncate: Boolean = KernelOptions.noTruncation, // false
                    showOutput: Boolean = KernelOptions.showOutput // true
                   ): (Option[Any], Option[String], Option[String]) = {
    if (interpreterOutput.isEmpty) {
      return (None, None, None)
    }

    var lastResult = Option.empty[Any]
    var lastResultAsString = ""
    val definitions = new StringBuilder
    val text = new StringBuilder

    interpreterOutput.split("\n").foreach {

      case HigherOrderFunction(name, func, funcType) =>

        definitions.append(s"$name: $func$funcType").append("\n")

      case NamedResult(name, vtype, value) if read(name).nonEmpty =>

        val result = read(name)

        lastResultAsString = result.map(String.valueOf(_)).getOrElse("")
        lastResult = result

        // magicOutput should be handled as result to properly
        // display based on MimeType.
        if(vtype != "org.apache.toree.magic.MagicOutput") {
          // default noTruncate = False
          // %truncation on ==>  noTruncate = false -> display Value
          // %truncation off ==>  noTruncate = true  -> display lastResultAsString
          val defLine = (showType, noTruncate) match {
            case (true, true) =>
              s"$name: $vtype = $lastResultAsString\n"
            case (true, false) =>
              lastResultAsString = value
              lastResult = Some(value)
              s"$name: $vtype = $value\n"
            case (false, true) =>
              s"$name = $lastResultAsString\n"
            case (false, false) =>
              lastResultAsString = value
              lastResult = Some(value)
              s"$name = $value\n"
          }

          // suppress interpreter-defined values
          if ( defLine.matches("res\\d+(.*)[\\S\\s]") == false ) {
            definitions.append(defLine)
          }

          if(showType) {
            if(defLine.startsWith("res")) {
              val v = defLine.split("^res\\d+(:|=)\\s+")(1)
              lastResultAsString = v
              lastResult = Some(v)
            } else {
              lastResultAsString = defLine
              lastResult = Some(defLine)
            }
          }
        }

      case Definition(defType, name) =>
        lastResultAsString = ""
        definitions.append(s"defined $defType $name\n")

      case Import(name) =>
        // do nothing with the line

      case line if lastResultAsString.contains(line) =>
        // do nothing with the line

      case line =>
        text.append(line).append("\n")
    }

    (lastResult,
     if (definitions.nonEmpty && showOutput) Some(definitions.toString) else None,
     if (text.nonEmpty && showOutput) Some(text.toString) else None)
  }

  protected def interpretBlock(code: String, silent: Boolean = false):
    (Results.Result, Either[ExecuteOutput, ExecuteFailure]) = {

     logger.trace(s"Interpreting line: $code")

     val futureResult = interpretAddTask(code, silent)

     // Map the old result types to our new types
     val mappedFutureResult = interpretMapToCustomResult(futureResult)

     // Determine whether to provide an error or output
     val futureResultAndExecuteInfo = interpretMapToResultAndOutput(mappedFutureResult)

     // Block indefinitely until our result has arrived
     import scala.concurrent.duration._
     Await.result(futureResultAndExecuteInfo, Duration.Inf)
   }

   protected def interpretMapToCustomResult(future: Future[IR.Result]): Future[Results.Result] = {
     import scala.concurrent.ExecutionContext.Implicits.global
     future map {
       case IR.Success             => Results.Success
       case IR.Error               => Results.Error
       case IR.Incomplete          => Results.Incomplete
     } recover {
       case ex: ExecutionException => Results.Aborted
     }
   }

   protected def interpretMapToResultAndOutput(future: Future[Results.Result]):
      Future[(Results.Result, Either[Map[String, String], ExecuteError])] = {
     import scala.concurrent.ExecutionContext.Implicits.global

     future map {
       case result @ (Results.Success | Results.Incomplete) =>
         val lastOutput = lastResultOut.toString("UTF-8").trim
         lastResultOut.reset()

         val (obj, defStr, text) = prepareResult(lastOutput, KernelOptions.showTypes, KernelOptions.noTruncation, KernelOptions.showOutput )
         defStr.foreach(kernel.display.content(MIMEType.PlainText, _))
         text.foreach(kernel.display.content(MIMEType.PlainText, _))
         val output = obj.map(Displayers.display(_).asScala.toMap).getOrElse(Map.empty)
         (result, Left(output))

       case Results.Error =>
         val lastOutput = lastResultOut.toString("UTF-8").trim
         lastResultOut.reset()

         val (obj, defStr, text) = prepareResult(lastOutput)
         defStr.foreach(kernel.display.content(MIMEType.PlainText, _))
         val output = interpretConstructExecuteError(text.get)
         (Results.Error, Right(output))

       case Results.Aborted =>
         (Results.Aborted, Right(null))
     }
   }

   def bindSparkContext() = {
     val bindName = "sc"

     doQuietly {
       logger.info(s"Binding SparkContext into interpreter as $bindName")
      interpret(s"""def ${bindName}: ${classOf[SparkContext].getName} = kernel.sparkContext""")

       // NOTE: This is needed because interpreter blows up after adding
       //       dependencies to SparkContext and Interpreter before the
       //       cluster has been used... not exactly sure why this is the case
       // TODO: Investigate why the cluster has to be initialized in the kernel
       //       to avoid the kernel's interpreter blowing up (must be done
       //       inside the interpreter)
       logger.debug("Initializing Spark cluster in interpreter")

//       doQuietly {
//         interpret(Seq(
//           "val $toBeNulled = {",
//           "  var $toBeNulled = sc.emptyRDD.collect()",
//           "  $toBeNulled = null",
//           "}"
//         ).mkString("\n").trim())
//       }
     }
   }

  def bindSparkSession(): Unit = {
    val bindName = "spark"

     doQuietly {
       // TODO: This only adds the context to the main interpreter AND
       //       is limited to the Scala interpreter interface
       logger.debug(s"Binding SparkSession into interpreter as $bindName")

      interpret(s"""def ${bindName}: ${classOf[SparkSession].getName} = kernel.sparkSession""")

//      interpret(
//        s"""
//           |def $bindName: ${classOf[SparkSession].getName} = {
//           |   if (org.apache.toree.kernel.interpreter.scala.InterpreterHelper.sparkSession != null) {
//           |     org.apache.toree.kernel.interpreter.scala.InterpreterHelper.sparkSession
//           |   } else {
//           |     val s = org.apache.spark.repl.Main.createSparkSession()
//           |     org.apache.toree.kernel.interpreter.scala.InterpreterHelper.sparkSession = s
//           |     s
//           |   }
//           |}
//         """.stripMargin)

     }
   }

  def defineImplicits(): Unit = {
    val code =
      """
        |import org.apache.spark.sql.SparkSession
        |import org.apache.spark.sql.SQLContext
        |import org.apache.spark.sql.SQLImplicits
        |
        |object implicits extends SQLImplicits with Serializable {
        |  protected override def _sqlContext: SQLContext = SparkSession.builder.getOrCreate.sqlContext
        |}
        |
        |import implicits._
      """.stripMargin
    doQuietly(interpret(code))
  }

  override def classLoader: ClassLoader = _runtimeClassloader

  /**
    * Returns the language metadata for syntax highlighting
    */
  override def languageInfo = LanguageInfo(
    "scala", BuildInfo.scalaVersion,
    fileExtension = Some(".scala"),
    pygmentsLexer = Some("scala"),
    mimeType = Some("text/x-scala"),
    codemirrorMode = Some("text/x-scala"))
}

object ScalaInterpreter {

  val HigherOrderFunction: Regex = """(\w+):\s+(\(\s*.*=>\s*\w+\))(\w+)\s*.*""".r
  val NamedResult: Regex = """(\w+):\s+([^=]+)\s+=\s*(.*)""".r
  val Definition: Regex = """defined\s+(\w+)\s+(.+)""".r
  val Import: Regex = """import\s+([\w\.,\{\}\s]+)""".r

  /**
    * Utility method to ensure that a temporary directory for the REPL exists for testing purposes.
    */
  def ensureTemporaryFolder(): String = {
    val outputDir = Option(System.getProperty("spark.repl.class.outputDir")).getOrElse({

      val execUri = System.getenv("SPARK_EXECUTOR_URI")
      val outputDir: String = Main.outputDir.getAbsolutePath
      System.setProperty("spark.repl.class.outputDir", outputDir)
      if (execUri != null) {
        System.setProperty("spark.executor.uri", execUri)
      }
      outputDir
    })
    outputDir
  }

}
