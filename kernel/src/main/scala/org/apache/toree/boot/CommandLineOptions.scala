/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package org.apache.toree.boot

import java.io.{File, OutputStream}

import com.typesafe.config.{Config, ConfigFactory}
import joptsimple.{OptionParser, OptionSpec}
import joptsimple.util.RegexMatcher._

import scala.collection.JavaConverters._

class CommandLineOptions(args: Seq[String]) {
  private val parser = new OptionParser()
  parser.allowsUnrecognizedOptions()

  /*
   * Options supported by our kernel.
   */
  private val _help =
    parser.acceptsAll(Seq("help", "h").asJava, "display help information").forHelp()

  private val _version =
    parser.acceptsAll(Seq("version", "v").asJava, "display version information")

  private val _profile =
    parser.accepts("profile", "path to IPython JSON connection file")
      .withRequiredArg().ofType(classOf[File])

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

  private val _default_repositories = parser.accepts(
    "default-repositories", "comma separated list of additional repositories to resolve"
  ).withRequiredArg().ofType(classOf[String])

  private val _default_repository_credentials = parser.accepts(
    "default-repository-credentials", "comma separated list of credential files to use"
  ).withRequiredArg().ofType(classOf[String])

  private val _max_interpreter_threads = parser.accepts(
    "max-interpreter-threads",
    "total number of worker threads to use to execute code"
  ).withRequiredArg().ofType(classOf[Int])

  private val _jar_dir = parser.accepts(
    "jar-dir",
    "directory where user added jars are stored (MUST EXIST)"
  ).withRequiredArg().ofType(classOf[String])

  private val _default_interpreter =
    parser.accepts("default-interpreter", "default interpreter for the kernel")
      .withRequiredArg().ofType(classOf[String])

  private val _nosparkcontext =
    parser.accepts("nosparkcontext", "Deprecated (see spark-context-initialization-mode). \n Kernel should not create a spark context")

  private val _spark_context_initialization_mode = parser.accepts(
    "spark-context-initialization-mode",
    "Identify how the Spark context initialization occurs. " +
      "NONE will not initialize a Spark Context," +
      "EAGER initialization will happen during runtime initialization,  " +
      "LAZY initialization will happen when the context is used for the first time."
  ).withRequiredArg().ofType(classOf[String]).withValuesConvertedBy( regex("(none)|(lazy)|(eager)")).defaultsTo("lazy")

  private val _spark_context_initialization_timeout = parser.accepts(
    "spark-context-initialization-timeout",
    "The time (in milliseconds) allowed for creation of the spark context. " +
      "Failure to create a context in this time could result in duplicate initialization messages. " +
      "The default value is 100 milliseconds."
  ).withRequiredArg().ofType(classOf[Long])

  private val _interpreter_plugin = parser.accepts(
    "interpreter-plugin"
  ).withRequiredArg().ofType(classOf[String])

  private val _alternate_sigint = parser.accepts(
    "alternate-sigint",
    "Specifies the signal to use instead of SIGINT for interrupting a long-running cell. " +
      "The value is a string and does not include the SIG prefix. Use of USR2 is recommended."
  ).withRequiredArg().ofType(classOf[String])

  private val options = parser.parse(args.map(_.trim): _*)

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

  val version: Boolean = has(_version)

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
      "stdin_port" -> get(_stdin_port),
      "shell_port" -> get(_shell_port),
      "iopub_port" -> get(_iopub_port),
      "control_port" -> get(_control_port),
      "hb_port" -> get(_heartbeat_port),
      "ip" -> get(_ip),
      "interpreter_args" -> interpreterArgs,
      "magic_urls" -> getAll(_magic_url).map(_.asJava)
        .flatMap(list => if (list.isEmpty) None else Some(list)),
      "max_interpreter_threads" -> get(_max_interpreter_threads),
      "alternate_sigint" -> get(_alternate_sigint),
      "jar_dir" -> get(_jar_dir),
      "default_interpreter" -> get(_default_interpreter),
      // deprecated in favor of spark-context-initialization-mode none
      // "nosparkcontext" -> (if (has(_nosparkcontext)) Some(true) else Some(false)),
      "spark_context_initialization_mode" -> (if( has(_spark_context_initialization_mode)) get(_spark_context_initialization_mode)
        else ( if (has(_nosparkcontext)) Some("none") else Some("lazy"))),
      "spark_context_initialization_timeout" -> get(_spark_context_initialization_timeout),
      "interpreter_plugins" -> interpreterPlugins,
      "default_repositories" -> getAll(_default_repositories).map(_.asJava)
        .flatMap(list => if (list.isEmpty) None else Some(list)),
      "default_repository_credentials" -> getAll(_default_repository_credentials).map(_.asJava)
          .flatMap(list => if (list.isEmpty) None else Some(list))

    ).flatMap(removeEmptyOptions).asInstanceOf[Map[String, AnyRef]].asJava)

    commandLineConfig.withFallback(profileConfig).withFallback(ConfigFactory.load)
  }

  private val removeEmptyOptions: ((String, Option[Any])) => Iterable[(String, Any)] = {
    pair => if (pair._2.isDefined) Some((pair._1, pair._2.get)) else None
  }

  private def interpreterArgs: Option[java.util.List[String]] = {
    val interpreterArgsList = args.dropWhile(_ != "--").drop(1).toList match {
      case Nil => List.empty[String]
      case list: List[String] => list
    }

    Some(interpreterArgsList.asJava)
  }

  private def interpreterPlugins: Option[java.util.List[String]] = {
    //val defaults = getAll(_default_interpreter_plugin).getOrElse(List())
    //val defaults = List[String](
    //  "SQL:org.apache.toree.kernel.interpreter.sql.SqlInterpreter"
    //)

    val userDefined = getAll(_interpreter_plugin) match {
      case Some(l) => l
      case _ => List[String]()
    }

    //val p = defaults ++ userDefined
    Some(userDefined.asJava)
  }

  /**
   * Prints the help message to the output stream provided.
    *
    * @param out The output stream to direct the help message
   */
  def printHelpOn(out: OutputStream) =
    parser.printHelpOn(out)
}

