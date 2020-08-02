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

package org.apache.toree.interpreter

import java.net.URL

import org.apache.toree.kernel.api.KernelLike

import scala.tools.nsc.interpreter._

case class LanguageInfo(
                 name: String,
                 version: String,
                 fileExtension: Option[String] = None,
                 pygmentsLexer: Option[String] = None,
                 mimeType: Option[String] = None,
                 codemirrorMode: Option[String] = None) {
}

trait Interpreter {

  /**
   * Initializes the interpreter.
   * @param kernel The kernel
   * @return The newly initialized interpreter
   */
  def init(kernel: KernelLike): Interpreter

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
   *         execution or the failure
   */
  def interpret(code: String, silent: Boolean = false, outputStreamResult: Option[OutputStream] = None):
    (Results.Result, Either[ExecuteOutput, ExecuteFailure])

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
   * Retrieves the contents of the variable with the provided name from the
   * interpreter.
   * @param variableName The name of the variable whose contents to read
   * @return An option containing the variable contents or None if the
   *         variable does not exist
   */
  def read(variableName: String): Option[Any]

  /**
   * Mask the Console and System objects with our wrapper implementations
   * and dump the Console methods into the public namespace (similar to
   * the Predef approach).
   * @param in The new input stream
   * @param out The new output stream
   * @param err The new error stream
   */
  def updatePrintStreams(in: InputStream, out: OutputStream, err: OutputStream)

  /**
   * Attempts to perform code completion via the <TAB> command.
   * @param code The current cell to complete
   * @param pos The cursor position
   * @return The cursor position and list of possible completions
   */
  def completion(code: String, pos: Int): (Int, List[String] ) = (pos, Nil)

  /**
    * Attempt to determine if a multiline block of code is complete
    * @param code The code to determine for completeness
    */
  def isComplete(code: String): (String, String) = ("unknown", "")

  /**
   * Returns the name of the variable created from the last execution.
   * @return Some String name if a variable was created, otherwise None
   */
  def lastExecutionVariableName: Option[String]

  /**
   * Returns the class loader used by this interpreter.
   * @return The runtime class loader used by this interpreter
   */
  def classLoader: ClassLoader

  /**
    * Returns the language metadata for syntax highlighting
    */
  def languageInfo: LanguageInfo

  /**
   * Initialization done after all other Toree initialization done.
   */

   def postInit (): Unit = {}
}
