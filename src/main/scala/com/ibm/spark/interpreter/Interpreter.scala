package com.ibm.spark.interpreter

import scala.tools.nsc.interpreter._

trait Interpreter {
  /**
   * Starts the interpreter, initializing any internal state.
   * @return A reference to the interpreter
   */
  def start: Interpreter

  /**
   * Stops the interpreter, removing any previous internal state.
   * @return A reference to the interpreter
   */
  def stop: Interpreter

  /**
   * Executes the provided code with the option to silence output.
   * @param code The code to execute
   * @param silent Whether or not to execute the code silently (no output)
   * @return The success/failure of the interpretation and the output from the
   *         execution
   */
  def interpret(code: String, silent: Boolean = false):
    (IR.Result, Either[ExecutionOutput, ExecutionError])
}
