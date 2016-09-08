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
import java.net.{URL, URLClassLoader}
import java.nio.charset.Charset
import java.util.concurrent.ExecutionException

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.repl.Main

import org.apache.toree.interpreter._
import org.apache.toree.kernel.api.{KernelLike, KernelOptions}
import org.apache.toree.utils.{MultiOutputStream, TaskManager}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.concurrent.{Await, Future}
import scala.language.reflectiveCalls
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.{IR, OutputStream}
import scala.tools.nsc.util.ClassPath
import scala.util.{Try => UtilTry}

class ScalaInterpreter(private val config:Config = ConfigFactory.load) extends Interpreter with ScalaInterpreterSpecific {
   protected val logger = LoggerFactory.getLogger(this.getClass.getName)

   protected val _thisClassloader = this.getClass.getClassLoader

   protected val lastResultOut = new ByteArrayOutputStream()

   protected val multiOutputStream = MultiOutputStream(List(Console.out, lastResultOut))
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
     val args = interpreterArgs(kernel)
     settings = newSettings(args)
     settings = appendClassPath(settings)

     start()
     bindKernelVariable(kernel)
     bindSparkSession()
     bindSparkContext()

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

     // Force dumping of current task (begin processing new tasks)
     taskManager.restart()

     this
   }

   override def interpret(code: String, silent: Boolean = false, output: Option[OutputStream]):
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
       logger.debug(s"Binding SQLContext into interpreter as $bindName")

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

   override def classLoader: ClassLoader = _runtimeClassloader
 }

object ScalaInterpreter {

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

