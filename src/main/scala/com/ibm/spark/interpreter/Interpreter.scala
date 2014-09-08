package com.ibm.spark.interpreter

import java.net.URL

import scala.tools.nsc.interpreter._

trait Interpreter {
  /**
   * Starts the interpreter, initializing any internal state.
   * @return A reference to the interpreter
   */
  def start(): Interpreter

  /**
   * Interrupts the current code being interpreted.
   * @return A reference to the interpreter
   */
  def interrupt(): Interpreter

  /**
   * Stops the interpreter, removing any previous internal state.
   * @return A reference to the interpreter
   */
  def stop(): Interpreter

  /**
   * Adds external jars to the internal classpaths of the interpreter.
   * @param jars The list of jar locations
   */
  def addJars(jars: URL*): Unit

  /**
   * Executes the provided code with the option to silence output.
   * @param code The code to execute
   * @param silent Whether or not to execute the code silently (no output)
   * @return The success/failure of the interpretation and the output from the
   *         execution
   */
  def interpret(code: String, silent: Boolean = false):
    (Results.Result, Either[ExecuteOutput, ExecuteError])

  /**
   * @return Returns a string to reference the URI of where the interpreted class files are created
   */
  def classServerURI: String

  /**
   * Executes body and will not print anything to the console during the execution
   * @param body The function to execute
   * @tparam T The return type of body
   * @return The return value of body
   */
  def doQuietly[T](body: => T): T

  /**
   * Binds a variable in the interpreter to a value.
   * @param variableName The name to expose the value in the interpreter
   * @param typeName The type of the variable, must be the fully qualified class name
   * @param value The value of the variable binding
   * @param modifiers Any annotation, scoping modifiers, etc on the variable
   */
  def bind(variableName: String, typeName: String, value: Any, modifiers: List[String])

  /**
   * Mask the Console and System objects with our wrapper implementations
   * and dump the Console methods into the public namespace (similar to
   * the Predef approach).
   * @param in The new input stream
   * @param out The new output stream
   * @param err The new error stream
   */
  def updatePrintStreams(in: InputStream, out: OutputStream, err: OutputStream)
}
