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

import java.io.{BufferedReader, ByteArrayOutputStream, InputStreamReader, PrintStream}
import java.net.{URL, URLClassLoader}
import java.nio.charset.Charset
import java.util.concurrent.ExecutionException

import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext
import org.apache.toree.global.StreamState
import org.apache.toree.interpreter._
import org.apache.toree.interpreter.imports.printers.{WrapperConsole, WrapperSystem}
import org.apache.toree.kernel.api.{KernelLike, KernelOptions}
import org.apache.toree.utils.{MultiOutputStream, TaskManager}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.concurrent.{Await, Future}
import scala.language.reflectiveCalls
import scala.tools.nsc.backend.JavaPlatform
import scala.tools.nsc.interpreter.{IR, InputStream, JPrintWriter, OutputStream}
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.util.{ClassPath, MergedClassPath}
import scala.tools.nsc.{Global, Settings, io}
import scala.util.{Try => UtilTry}

class ScalaInterpreter() extends Interpreter with ScalaInterpreterSpecific {
   protected val logger = LoggerFactory.getLogger(this.getClass.getName)

   protected var settings: Settings = null

   protected val _thisClassloader = this.getClass.getClassLoader
   protected val _runtimeClassloader =
     new URLClassLoader(Array(), _thisClassloader) {
       def addJar(url: URL) = this.addURL(url)
     }

   protected val lastResultOut = new ByteArrayOutputStream()
   protected val multiOutputStream = MultiOutputStream(List(Console.out, lastResultOut))
   private[scala] var taskManager: TaskManager = _

   private var maxInterpreterThreads:Int = TaskManager.DefaultMaximumWorkers

   protected def newTaskManager(): TaskManager =
     new TaskManager(maximumWorkers = maxInterpreterThreads)

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
     require(taskManager != null)

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

   override def classLoader: ClassLoader = _runtimeClassloader
 }

