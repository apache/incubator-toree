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

package org.apache.toree.utils

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

  // NOTE: Cannot be implicit as conflicts with get
  def getAll[T](spec: OptionSpec[T]): Option[List[T]] = {
    require(options != null, "Arguments not parsed yet!")
    Some(options.valuesOf(spec).asScala.toList).filter(_ != null)
  }
}
