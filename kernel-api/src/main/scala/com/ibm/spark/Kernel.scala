package com.ibm.spark

import com.ibm.spark.interpreter._

class Kernel (
  val interpreter: Interpreter
) {
  def eval(code: Option[String]): (Boolean, String) = {
    code.map(c => {
      val (success, result) = interpreter.interpret(c)
      success match {
        case Results.Success =>
          (true, result.left.getOrElse("").asInstanceOf[String])
        case Results.Error =>
          (false, result.right.getOrElse("").toString)
        case Results.Aborted =>
          (false, "Aborted!")
        case Results.Incomplete =>
          // If we get an incomplete it's most likely a syntax error, so
          // let the user know.
          (false, "Syntax Error!")
      }
    }).getOrElse((false, "Error!"))
  }
}