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

package org.apache.toree.magic.builtin

import java.io.PrintStream
import java.net.URL

import org.apache.toree.magic._
import org.apache.toree.magic.dependencies._
import org.apache.toree.utils.ArgumentParsingSupport
import scala.util.Try
import org.apache.toree.plugins.annotations.Event

class AddDeps extends LineMagic with IncludeInterpreter
  with IncludeOutputStream with IncludeSparkContext with ArgumentParsingSupport
  with IncludeDependencyDownloader with IncludeKernel
{

  private def printStream = new PrintStream(outputStream)

  private val _transitive = parser.accepts(
    "transitive", "Retrieve dependencies recursively"
  )

  private val _verbose = parser.accepts(
    "verbose", "Prints out additional information"
  )

  private val _trace = parser.accepts(
    "trace", "Prints out trace of download progress"
  )

  private val _abortOnResolutionErrors = parser.accepts(
    "abort-on-resolution-errors", "Abort (no downloads) when resolution fails"
  )

  private val _repository = parser.accepts(
    "repository", "Adds an additional repository to available list"
  ).withRequiredArg().ofType(classOf[String])

  /**
   * Execute a magic representing a line magic.
   *
   * @param code The single line of code
   * @return The output of the magic
   */
  @Event(name = "adddeps")
  override def execute(code: String): Unit = {
    val nonOptionArgs = parseArgs(code)
    dependencyDownloader.setPrintStream(printStream)

    val extraRepositories = getAll(_repository).getOrElse(Nil).map(u => (u, Try(new URL(u))))

    // Print error information
    extraRepositories.filter(_._2.isFailure).map(_._1)
      .foreach(u => printStream.println(s"Ignoring invalid URL $u"))

    if (nonOptionArgs.size == 3) {
      // get the jars and hold onto the paths at which they reside
      val urls = dependencyDownloader.retrieve(
        groupId                 = nonOptionArgs.head,
        artifactId              = nonOptionArgs(1),
        version                 = nonOptionArgs(2),
        transitive              = _transitive,
        ignoreResolutionErrors  = !_abortOnResolutionErrors,
        extraRepositories       = extraRepositories.flatMap(_._2.toOption),
        verbose                 = _verbose,
        trace                   = _trace
      ).map(_.toURL)

      // add the jars to the interpreter and spark context
      interpreter.addJars(urls:_*)
      urls.foreach(url => sparkContext.addJar(url.getPath))
    } else {
      printHelp(printStream, """%AddDeps my.company artifact-id version""")
    }
  }
}
