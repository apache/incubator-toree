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

import java.io._
import java.net.URL

import org.apache.toree.global.StreamState
import org.apache.toree.interpreter.InterpreterTypes.ExecuteOutput
import org.apache.toree.interpreter.imports.printers.{WrapperConsole, WrapperSystem}
import org.apache.toree.interpreter.{ExecuteError, ExecuteFailure, Results, Interpreter}
import scala.tools.nsc.interpreter.{JPrintWriter, IMain, JLineCompletion, IR}

import scala.concurrent.Future
import scala.tools.nsc.Settings

trait ScalaInterpreterSpecific { this: ScalaInterpreter =>
  private val ExecutionExceptionName = "lastException"

  private var iMain: IMain = _
  private var jLineCompleter: JLineCompletion = _

  protected def newIMain(settings: Settings, out: JPrintWriter): IMain = {
    val s = new IMain(settings, out)
    s.initializeSynchronous()
    s
  }

  /**
   * Adds jars to the runtime and compile time classpaths. Does not work with
   * directories or expanding star in a path.
   * @param jars The list of jar locations
   */
  override def addJars(jars: URL*): Unit = {
    jars.foreach(_runtimeClassloader.addJar)
    iMain.addUrlsToClassPath(jars: _*)
  }

  /**
   * Binds a variable in the interpreter to a value.
   * @param variableName The name to expose the value in the interpreter
   * @param typeName The type of the variable, must be the fully qualified class name
   * @param value The value of the variable binding
   * @param modifiers Any annotation, scoping modifiers, etc on the variable
   */
  override def bind(
    variableName: String,
    typeName: String,
    value: Any,
    modifiers: List[String]
  ): Unit = {
    require(iMain != null)
    iMain.bind(variableName, typeName, value, modifiers)
  }


  /**
   * Executes body and will not print anything to the console during the execution
   * @param body The function to execute
   * @tparam T The return type of body
   * @return The return value of body
   */
  override def doQuietly[T](body: => T): T = {
    require(iMain != null)
    iMain.beQuietDuring[T](body)
  }


  /**
   * Stops the interpreter, removing any previous internal state.
   * @return A reference to the interpreter
   */
  override def stop(): Interpreter = {
    logger.info("Shutting down interpreter")

    // Shut down the task manager (kills current execution
    if (taskManager != null) taskManager.stop()
    taskManager = null

    // Erase our completer
    jLineCompleter = null

    // Close the entire interpreter (loses all state)
    if (iMain != null) iMain.close()
    iMain = null

    this
  }


  /**
   * Not available on Scala 2.11!
   */
  override def classServerURI: String = ???

  /**
   * Returns the name of the variable created from the last execution.
   * @return Some String name if a variable was created, otherwise None
   */
  override def lastExecutionVariableName: Option[String] = {
    require(iMain != null)

    // TODO: Get this API method changed back to public in Apache Spark
    val lastRequestMethod = classOf[IMain].getDeclaredMethod("lastRequest")
    lastRequestMethod.setAccessible(true)

    val mostRecentVariableName = iMain.mostRecentVar

    iMain.allDefinedNames.map(_.toString).find(_ == mostRecentVariableName)
  }

  /**
   * Mask the Console and System objects with our wrapper implementations
   * and dump the Console methods into the public namespace (similar to
   * the Predef approach).
   * @param in The new input stream
   * @param out The new output stream
   * @param err The new error stream
   */
  override def updatePrintStreams(
    in: InputStream,
    out: OutputStream,
    err: OutputStream
  ): Unit = {
    val inReader = new BufferedReader(new InputStreamReader(in))
    val outPrinter = new PrintStream(out)
    val errPrinter = new PrintStream(err)

    iMain.beQuietDuring {
      iMain.bind(
        "Console", classOf[WrapperConsole].getName,
        new WrapperConsole(inReader, outPrinter, errPrinter),
        List("""@transient""")
      )
      iMain.bind(
        "System", classOf[WrapperSystem].getName,
        new WrapperSystem(in, out, err),
        List("""@transient""")
      )
      iMain.interpret("import Console._")
    }
  }

  /**
   * Retrieves the contents of the variable with the provided name from the
   * interpreter.
   * @param variableName The name of the variable whose contents to read
   * @return An option containing the variable contents or None if the
   *         variable does not exist
   */
  override def read(variableName: String): Option[AnyRef] = {
    require(iMain != null)
    val variable = iMain.valueOfTerm(variableName)
    if (variable == null || variable.isEmpty) None
    else variable match {
      case Some(v: AnyRef)  => Some(v)
      case Some(_)          => None // Don't support AnyVal yet
      case None             => None
    }
  }

  /**
   * Starts the interpreter, initializing any internal state.
   * @return A reference to the interpreter
   */
  override def start(): Interpreter = {
    require(iMain == null && taskManager == null)

    taskManager = newTaskManager()

    logger.debug("Initializing task manager")
    taskManager.start()

    iMain = newIMain(settings, new JPrintWriter(multiOutputStream, true))

    //logger.debug("Initializing interpreter")
    //iMain.initializeSynchronous()

    logger.debug("Initializing completer")
    jLineCompleter = new JLineCompletion(iMain)

    iMain.beQuietDuring {
      //logger.info("Rerouting Console and System related input and output")
      //updatePrintStreams(System.in, multiOutputStream, multiOutputStream)

      //   ADD IMPORTS generates too many classes, client is responsible for adding import
      logger.debug("Adding org.apache.spark.SparkContext._ to imports")
      iMain.interpret("import org.apache.spark.SparkContext._")
    }

    this
  }

  /**
   * Attempts to perform code completion via the <TAB> command.
   * @param code The current cell to complete
   * @param pos The cursor position
   * @return The cursor position and list of possible completions
   */
  override def completion(code: String, pos: Int): (Int, List[String]) = {
    require(jLineCompleter != null)

    logger.debug(s"Attempting code completion for ${code}")
    val regex = """[0-9a-zA-Z._]+$""".r
    val parsedCode = (regex findAllIn code).mkString("")

    logger.debug(s"Attempting code completion for ${parsedCode}")
    val result = jLineCompleter.completer().complete(parsedCode, pos)

    (result.cursor, result.candidates)
  }

  protected def newSettings(args: List[String]): Settings = ???

  protected def interpretAddTask(code: String, silent: Boolean): Future[IR.Result] = {
    taskManager.add {
      // Add a task using the given state of our streams
      StreamState.withStreams {
        if (silent) {
          iMain.beSilentDuring {
            iMain.interpret(code)
          }
        } else {
          iMain.interpret(code)
        }
      }
    }
  }

  protected def interpretMapToResultAndExecuteInfo(
    future: Future[(Results.Result, String)]
  ): Future[(Results.Result, Either[ExecuteOutput, ExecuteFailure])] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    future map {
      case (Results.Success, output)    => (Results.Success, Left(output))
      case (Results.Incomplete, output) => (Results.Incomplete, Left(output))
      case (Results.Aborted, output)    => (Results.Aborted, Right(null))
      case (Results.Error, output)      =>
        (
          Results.Error,
          Right(
            interpretConstructExecuteError(
              read(ExecutionExceptionName),
              output
            )
          )
        )
    }
  }

  protected def interpretConstructExecuteError(
    value: Option[AnyRef],
    output: String
  ) = value match {
    // Runtime error
    case Some(e) if e != null =>
      val ex = e.asInstanceOf[Throwable]
      // Clear runtime error message
      iMain.directBind(
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
      if (iMain.reporter.hasErrors)
      // TODO: This wrapper is not needed when just getting compile
      // error that we are not parsing... maybe have it be purely
      // output and have the error check this?
        ExecuteError(
          "Compile Error", output, List()
        )
      else
        ExecuteError("Unknown", "Unable to retrieve error!", List())
  }
}
