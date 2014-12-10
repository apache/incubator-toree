package com.ibm.spark.interpreter.imports.printers

import java.io._

import com.ibm.spark.utils.DynamicReflectionSupport

/**
 * Represents a wrapper for the scala.Console for Scala 2.10.4 implementation.
 * @param in The input stream used for standard in
 * @param out The output stream used for standard out
 * @param err The output stream used for standard error
 */
class WrapperConsole(
  val in: BufferedReader,
  val out: PrintStream,
  val err: PrintStream
) extends DynamicReflectionSupport(Class.forName("scala.Console$"), scala.Console) {
  require(in != null)
  require(out != null)
  require(err != null)

  //
  // SUPPORTED PRINT OPERATIONS
  //

  def print(obj: Any): Unit = out.print(obj)
  def printf(text: String, args: Any*): Unit =
    out.print(text.format(args: _*))
  def println(x: Any): Unit = out.println(x)
  def println(): Unit = out.println()
}
