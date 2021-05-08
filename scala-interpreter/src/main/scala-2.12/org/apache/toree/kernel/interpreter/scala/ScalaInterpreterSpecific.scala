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
import org.apache.toree.interpreter.imports.printers.{WrapperConsole, WrapperSystem}
import org.apache.toree.interpreter.{ExecuteError, Interpreter}
import scala.tools.nsc.interpreter._
import scala.concurrent.Future
import scala.tools.nsc.{Global, Settings, util}
import scala.util.Try

trait ScalaInterpreterSpecific extends SettingsProducerLike { this: ScalaInterpreter =>
  private val ExecutionExceptionName = "lastException"

  private[toree] var iMain: IMain = _
  private var completer: PresentationCompilerCompleter = _
  private val exceptionHack = new ExceptionHack()

  def _runtimeClassloader = {
    _thisClassloader
  }

  protected def newIMain(settings: Settings, out: JPrintWriter): IMain = {
    val s = new IMain(settings, out)
    s.initializeSynchronous()
    s
  }

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
    val termSymbol = iMain.symbolOfTerm(termNameString)


    convertAnnotationsToModifiers(
      if (termSymbol.hasAccessorFlag) termSymbol.accessed.annotations
      else termSymbol.annotations
    ) ++ convertScopeToModifiers(termSymbol)
  }


  protected def refreshDefinitions(): Unit = {
    iMain.definedTerms.foreach(termName => {
      val termNameString = termName.toString
      val termTypeString = iMain.typeOfTerm(termNameString).toLongString
      iMain.valueOfTerm(termNameString) match {
        case Some(termValue)  =>
          val modifiers = buildModifierList(termNameString)
          logger.debug(s"Rebinding of $termNameString as " +
            s"${modifiers.mkString(" ")} $termTypeString")
          Try(iMain.beSilentDuring {
            iMain.bind(
              termNameString, termTypeString, termValue, modifiers
            )
          })
        case None             =>
          logger.debug(s"Ignoring rebinding of $termNameString")
      }
    })
  }

  protected def reinitializeSymbols(): Unit = {
    val global = iMain.global
    import global._
    new Run // Initializes something needed for Scala classes
  }

  /**
   * Adds jars to the runtime and compile time classpaths. Does not work with
   * directories or expanding star in a path.
   * @param jars The list of jar locations
   */
  override def addJars(jars: URL*): Unit = {
    iMain.addUrlsToClassPath(jars:_*)
    // the Scala interpreter will invalidate definitions for any package defined in
    // the new Jars. This can easily include org.* and make the kernel inaccessible
    // because it is bound using the previous package definition. To avoid problems,
    // it is necessary to refresh variable definitions to use the new packages and
    // to rebind the global definitions.
    refreshDefinitions()
    bindVariables()
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
    logger.warn(s"Binding $modifiers $variableName $typeName $value")
    require(iMain != null)
    val sIMain = iMain

    val bindRep = new sIMain.ReadEvalPrint()
    iMain.interpret(s"import $typeName")
    bindRep.compile("""
                      |object %s {
                      |  var value: %s = _
                      |  def set(x: Any) = value = x.asInstanceOf[%s]
                      |}
                    """.stripMargin.format(bindRep.evalName, typeName, typeName)
    )
    bindRep.callEither("set", value) match {
      case Left(ex) =>
        logger.error("Set failed in bind(%s, %s, %s)".format(variableName, typeName, value))
        logger.error(util.stackTraceString(ex))
        IR.Error

      case Right(_) =>
        val line = "%sval %s = %s.value".format(modifiers map (_ + " ") mkString, variableName, bindRep.evalPath)
        logger.debug("Interpreting: " + line)
        iMain.interpret(line)
    }

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
    completer = null

    // Close the entire interpreter (loses all state)
    if (iMain != null) iMain.close()
    iMain = null

    this
  }

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
  override def read(variableName: String): Option[Any] = {
    require(iMain != null)

    try {
      iMain.valueOfTerm(variableName)
    } catch {
      // if any error returns None
      case e: Throwable => {
        logger.debug(s"Error reading variable name: ${variableName}", e)
        clearLastException()
        None
      }
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

    iMain = newIMain(settings, new JPrintWriter(lastResultOut, true))

    //logger.debug("Initializing interpreter")
    //iMain.initializeSynchronous()

    logger.debug("Initializing completer")
    completer = new PresentationCompilerCompleter(iMain)

    iMain.beQuietDuring {
      //logger.info("Rerouting Console and System related input and output")
      //updatePrintStreams(System.in, multiOutputStream, multiOutputStream)

      //   ADD IMPORTS generates too many classes, client is responsible for adding import
      logger.debug("Adding org.apache.spark.SparkContext._ to imports")
      iMain.interpret("import org.apache.spark.SparkContext._")

      logger.debug("Adding the hack for the exception handling retrieval.")
      iMain.bind("_exceptionHack", classOf[ExceptionHack].getName, exceptionHack, List("@transient"))
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

    require(completer != null)

    logger.debug(s"Attempting code completion for ${code}")
    val result = completer.complete(code, pos)

    (result.cursor, result.candidates)
  }

  /**
    * Attempts to perform completeness checking for a statement by seeing if we can parse it
    * using the scala parser.
    *
    * @param code The current cell to complete
    * @return tuple of (completeStatus, indent)
    */
  override def isComplete(code: String): (String, String) = {
    val result = iMain.beSilentDuring {
      val parse = iMain.parse
      parse(code) match {
        case t: parse.Error => ("invalid", "")
        case t: parse.Success =>
          val lines = code.split("\n", -1)
          val numLines = lines.length
          // for multiline code blocks, require an empty line before executing
          // to mimic the behavior of ipython
          if (numLines > 1 && lines.last.matches("\\s*\\S.*")) {
            ("incomplete", startingWhiteSpace(lines.last))
          } else {
            ("complete", "")
          }
        case t: parse.Incomplete =>
          val lines = code.split("\n", -1)
          // For now lets just grab the indent of the current line, if none default to 2 spaces.
          ("incomplete", startingWhiteSpace(lines.last))
      }
    }
    lastResultOut.reset()
    result
  }

  private def startingWhiteSpace(line: String): String = {
    val indent = "^\\s+".r.findFirstIn(line).getOrElse("")
    // increase the indent if the line ends with => or {
    if (line.matches(".*(?:(?:\\{)|(?:=>))\\s*")) {
      indent + "  "
    } else {
      indent
    }
  }

  override def newSettings(args: List[String]): Settings = {
    val s = new Settings()

    val dir = ScalaInterpreter.ensureTemporaryFolder()

    s.processArguments(args ++
      List(
        "-Yrepl-class-based",
        "-Yrepl-outdir", s"$dir"
        // useful for debugging compiler classpath or package issues
        // "-uniqid", "-explaintypes", "-usejavacp", "-Ylog-classpath"
    ), processAll = true)
    s
  }

  protected def interpretAddTask(code: String, silent: Boolean): Future[IR.Result] = {
    if (iMain == null) throw new IllegalArgumentException("Interpreter not started yet!")

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

  private def retrieveLastException: Throwable = {
    iMain.beSilentDuring {
      iMain.interpret("_exceptionHack.lastException = lastException")
    }
    exceptionHack.lastException
  }

  private def clearLastException(): Unit = {
    iMain.directBind(
      ExecutionExceptionName,
      classOf[Throwable].getName,
      null
    )
    exceptionHack.lastException = null
  }

  protected def interpretConstructExecuteError(output: String) = {
    Option(retrieveLastException) match {
      // Runtime error
      case Some(e) =>
        val ex = e.asInstanceOf[Throwable]
        clearLastException()

        // The scala REPL does a pretty good job of returning us a stack trace that is free from all the bits that the
        // interpreter uses before it.
        //
        // The REPL emits its message as something like this, so trim off the first and last element
        //
        //    java.lang.ArithmeticException: / by zero
        //    at failure(<console>:17)
        //    at call_failure(<console>:19)
        //    ... 40 elided

        val formattedException = output.split("\n")

        ExecuteError(
          ex.getClass.getName,
          ex.getLocalizedMessage,
          formattedException.toList
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
        // May as capture the output here.  Could be useful
          ExecuteError("Unknown Error", output, List())
    }
  }
}

/**
  * Due to a bug in the scala interpreter under scala 2.11 (SI-8935) with IMain.valueOfTerm we can hack around it by
  * binding an instance of ExceptionHack into iMain and interpret the "_exceptionHack.lastException = lastException".
  * This makes it possible to extract the exception.
  *
  * TODO: Revisit this once Scala 2.12 is released.
  */
class ExceptionHack {
  var lastException: Throwable = _
}
