/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.spark.boot

import java.io.{File, OutputStream}

import com.typesafe.config.{Config, ConfigFactory}
import joptsimple.{OptionParser, OptionSpec}

import scala.collection.JavaConverters._

class CommandLineOptions(args: Seq[String]) {
  private val parser = new OptionParser()
  parser.allowsUnrecognizedOptions()

  /*
   * Options supported by our kernel.
   */
  private val _help =
    parser.accepts("help", "display help information").forHelp()

  private val _profile =
    parser.accepts("profile", "path to IPython JSON connection file")
      .withRequiredArg().ofType(classOf[File])

  private val _master =
    parser.accepts("master", "location of master Spark node")
      .withRequiredArg().ofType(classOf[String])

  private val _ip =
    parser.accepts("ip", "ip used to bind sockets")
      .withRequiredArg().ofType(classOf[String])

  private val _stdin_port = parser.accepts(
    "stdin-port", "port of the stdin socket"
  ).withRequiredArg().ofType(classOf[Int])

  private val _shell_port = parser.accepts(
    "shell-port", "port of the shell socket"
  ).withRequiredArg().ofType(classOf[Int])

  private val _iopub_port = parser.accepts(
    "iopub-port", "port of the iopub socket"
  ).withRequiredArg().ofType(classOf[Int])

  private val _control_port = parser.accepts(
    "control-port", "port of the control socket"
  ).withRequiredArg().ofType(classOf[Int])

  private val _heartbeat_port = parser.accepts(
    "heartbeat-port", "port of the heartbeat socket"
  ).withRequiredArg().ofType(classOf[Int])

  private val _magic_url =
    parser.accepts("magic-url", "path to a magic jar")
      .withRequiredArg().ofType(classOf[String])

  private val options = parser.parse(args: _*)

  /*
   * Helpers to determine if an option is provided and the value with which it
   * was provided.
   */

  private def has[T](spec: OptionSpec[T]): Boolean =
    options.has(spec)

  private def get[T](spec: OptionSpec[T]): Option[T] =
    Some(options.valueOf(spec)).filter(_ != null)

  private def getAll[T](spec: OptionSpec[T]): Option[List[T]] =
    Some(options.valuesOf(spec).asScala.toList).filter(_ != null)

  /*
   * Expose options in terms of their existence/value.
   */

  val help: Boolean = has(_help)

  /*
   * Config object has 3 levels and fallback in this order
   * 1. Comandline Args
   * 2. --profile file
   * 3. Defaults
   */
  def toConfig: Config = {
    val profileConfig: Config = get(_profile) match {
      case Some(x) =>
        ConfigFactory.parseFile(x)
      case None =>
        ConfigFactory.empty()
    }

    val commandLineConfig: Config = ConfigFactory.parseMap(Map(
        "spark.master" -> get(_master),
        "stdin_port" -> get(_stdin_port),
        "shell_port" -> get(_shell_port),
        "iopub_port" -> get(_iopub_port),
        "control_port" -> get(_control_port),
        "hb_port" -> get(_heartbeat_port),
        "ip" -> get(_ip),
        "interpreter_args" -> interpreterArgs,
        "magic_urls" -> Some(getAll(_magic_url).get.asJava)
    ).flatMap(removeEmptyOptions).asInstanceOf[Map[String, AnyRef]].asJava)

    commandLineConfig.withFallback(profileConfig).withFallback(ConfigFactory.load)
  }

  private def removeEmptyOptions: ((String, Option[Any])) => Iterable[(String, Any)] = {
    pair => if (pair._2.isDefined) Some((pair._1, pair._2.get)) else None
  }

  /**
   *
   * @return
   */
  private def interpreterArgs: Option[java.util.List[String]] = {
    args.dropWhile(_ != "--").drop(1).toList match {
      case Nil => None
      case list: List[String] => Some(list.asJava)
    }
  }

  /**
   * Prints the help message to the output stream provided.
   * @param out The output stream to direct the help message
   */
  def printHelpOn(out: OutputStream) =
    parser.printHelpOn(out)
}

