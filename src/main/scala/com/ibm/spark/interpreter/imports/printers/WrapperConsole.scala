package com.ibm.spark.interpreter.imports.printers

import java.io._

import com.ibm.spark.utils.DynamicSupport

/**
 * Represents a wrapper for the scala.Console for Scala 2.10.4 implementation.
 * @param in The input stream used for standard in
 * @param out The output stream used for standard out
 * @param err The output stream used for standard error
 */
class WrapperConsole(
  val in: InputStream,
  val out: OutputStream,
  val err: OutputStream
) extends DynamicSupport(Class.forName("scala.Console$"), scala.Console) {
  require(in != null)
  require(out != null)
  require(err != null)

  private val inReader = new BufferedReader(new InputStreamReader(in))
  private val outPrinter = new PrintStream(out)
  private val errPrinter = new PrintStream(err)

  //
  // SUPPORTED PRINT OPERATIONS
  //

  def print(obj: Any): Unit = outPrinter.print(obj)
  def printf(text: String, args: Any*): Unit =
    outPrinter.print(text.format(args: _*))
  def println(x: Any): Unit = outPrinter.println(x)
  def println(): Unit = outPrinter.println
}
