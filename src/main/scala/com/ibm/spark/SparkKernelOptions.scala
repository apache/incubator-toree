package com.ibm.spark

import java.io.{OutputStream, File}
import java.util
import joptsimple.OptionParser
import joptsimple.OptionSpec

class SparkKernelOptions(args: Seq[String]) {
  private val DefaultMasterArg = "local[*]"
  private val parser = new OptionParser()

  /*
   * Options supported by our kernel.
   */
  private val _help =
    parser.accepts("help", "display help information").forHelp()
  private val _verbose =
    parser.accepts("verbose", "print debugging information")
  private val _create_context =
    parser.accepts("create-context", "whether to create a context or not")
      .withOptionalArg().ofType(classOf[Boolean])
      .defaultsTo(true)
  private val _profile =
    parser.accepts("profile", "path to IPython JSON connection file")
      .withRequiredArg().ofType(classOf[File])
  private val _master =
    parser.accepts("master", "location of master Spark node")
      .withRequiredArg().ofType(classOf[String])
      .defaultsTo(DefaultMasterArg)
  private val options = parser.parse(args: _*)

  /*
   * Helpers to determine if an option is provided and the value with which it
   * was provided.
   */

  private def has[T](spec: OptionSpec[T]): Boolean =
    options.has(spec)

  private def get[T](spec: OptionSpec[T]): Option[T] =
    Some(options.valueOf(spec)).filter(_ != null)

  /*
   * Expose options in terms of their existence/value.
   */

  val help: Boolean = has(_help)
  val verbose: Boolean = has(_verbose)
  val create_context: Boolean = get(_create_context) match {
    case Some(value) => value
    case None => true // Default value
  }
  val profile: Option[File] = get(_profile)
  val master: Option[String] = get(_master)

  /**
   *
   * @return
   */
  def tail = args.dropWhile(_ != "--").drop(1).toList

  /**
   * Prints the help message to the output stream provided.
   * @param out The output stream to direct the help message
   */
  def printHelpOn(out: OutputStream) =
    parser.printHelpOn(out)
}

