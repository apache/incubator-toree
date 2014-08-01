package com.ibm.spark.interpreter.imports.printers

import java.io._

import com.ibm.spark.utils.DynamicSupport

/**
 * Represents a wrapper for java.lang.System.
 * @param inStream The input stream used for standard in
 * @param outStream The output stream used for standard out
 * @param errStream The output stream used for standard error
 */
class WrapperSystem(
  private val inStream: InputStream,
  private val outStream: OutputStream,
  private val errStream: OutputStream
) extends DynamicSupport(Class.forName("java.lang.System"), null){
  require(inStream != null)
  require(outStream != null)
  require(errStream != null)

  private val outPrinter = new PrintStream(outStream)
  private val errPrinter = new PrintStream(errStream)

  //
  // MASKED METHODS
  //

  def in = inStream
  def out = outPrinter
  def err = errPrinter
}
