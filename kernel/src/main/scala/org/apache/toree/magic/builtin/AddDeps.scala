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

import java.io.{File, PrintStream}
import java.net.URL

import org.apache.toree.dependencies.Credentials
import org.apache.toree.magic._
import org.apache.toree.magic.dependencies._
import org.apache.toree.utils.ArgumentParsingSupport

import scala.util.Try
import org.apache.toree.plugins.annotations.Event


class AddDeps extends LineMagic with IncludeInterpreter
  with IncludeOutputStream with ArgumentParsingSupport
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

  private val _exclude = parser.accepts("exclude", "exclude dependency").withRequiredArg().ofType(classOf[String])

  private val _repository = parser.accepts(
    "repository", "Adds an additional repository to available list"
  ).withRequiredArg().ofType(classOf[String])

  private val _credentials = parser.accepts(
    "credential", "Adds a credential file to be used to the list"
  ).withRequiredArg().ofType(classOf[String])

  private val _configuration = parser.accepts(
    "ivy-configuration", "Sets the Ivy configuration for the dependency; defaults to \"default\""
  ).withRequiredArg().ofType(classOf[String])

  private val _classifier = parser.accepts(
    "classifier", "Sets the dependency's classifier"
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

    val repository = getAll(_repository).getOrElse(Nil)
    val credentials = getAll(_credentials).getOrElse(Nil)
    val excludes = getAll(_exclude).getOrElse(Nil)

    val excludesSet = excludes.map((x: String) => {
      if (x.contains(":")) {
        (x.split(":")(0), x.split(":")(1))
      } else {
        (x, "*")
      }
    }: (String, String)).toSet

    val repositoriesWithCreds = dependencyDownloader.resolveRepositoriesAndCredentials(repository, credentials)

    if (nonOptionArgs.size == 3) {
      // get the jars and hold onto the paths at which they reside
      val uris = dependencyDownloader.retrieve(
        groupId                 = nonOptionArgs.head,
        artifactId              = nonOptionArgs(1),
        version                 = nonOptionArgs(2),
        transitive              = _transitive,
        ignoreResolutionErrors  = !_abortOnResolutionErrors,
        extraRepositories       = repositoriesWithCreds,
        verbose                 = _verbose,
        trace                   = _trace,
        excludes                = excludesSet,
        configuration           = get(_configuration),
        artifactClassifier      = get(_classifier)
      )

      // pass the new Jars to the kernel
      kernel.addJars(uris.filter(_.getPath.endsWith(".jar")): _*)
    } else {
      printHelp(printStream, """%AddDeps my.company artifact-id version""")
    }
  }


}
