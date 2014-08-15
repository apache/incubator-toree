package com.ibm.spark.magic.dependencies

import com.ibm.spark.interpreter.Interpreter
import com.ibm.spark.magic.builtin.MagicTemplate

trait IncludeInterpreter {
  this: MagicTemplate =>

  //val interpreter: Interpreter
  private var _interpreter: Interpreter = _
  def interpreter: Interpreter = _interpreter
  def interpreter_=(newInterpreter: Interpreter) = _interpreter = newInterpreter
}
