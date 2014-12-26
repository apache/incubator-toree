package com.ibm.spark.kernel.api

import com.ibm.spark.comm.CommManager
import com.ibm.spark.interpreter._

/**
 * Represents the main kernel API to be used for interaction.
 *
 * @param interpreter The interpreter to expose in this instance
 * @param comm The Comm manager to expose in this instance
 */
class Kernel (
  val interpreter: Interpreter,
  val comm: CommManager
) extends KernelLike {
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