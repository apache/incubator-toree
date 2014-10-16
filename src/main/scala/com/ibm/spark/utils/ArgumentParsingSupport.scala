package com.ibm.spark.utils

import joptsimple.{OptionSpec, OptionParser}
import scala.collection.JavaConverters._
import scala.language.implicitConversions
import java.io.{PrintStream, OutputStream}

trait ArgumentParsingSupport {
  protected lazy val parser = new OptionParser()
  private var options: joptsimple.OptionSet = _
  parser.allowsUnrecognizedOptions()

  /**
   * Parses the arguments provided as a string, updating all internal
   * references to specific arguments.
   *
   * @param args The arguments as a string
   * @param delimiter An optional delimiter for separating arguments
   */
  def parseArgs(args: String, delimiter: String = " ") = {
    options = parser.parse(args.split(delimiter): _*)

    options.nonOptionArguments().asScala.map(_.toString)
  }

  def printHelp(outputStream: OutputStream, usage: String) = {
    val printStream = new PrintStream(outputStream)

    printStream.println(s"Usage: $usage\n")
    parser.printHelpOn(outputStream)
  }

  implicit def has[T](spec: OptionSpec[T]): Boolean = {
    require(options != null, "Arguments not parsed yet!")
    options.has(spec)
  }

  implicit def get[T](spec: OptionSpec[T]): Option[T] = {
    require(options != null, "Arguments not parsed yet!")
    Some(options.valueOf(spec)).filter(_ != null)
  }
}
