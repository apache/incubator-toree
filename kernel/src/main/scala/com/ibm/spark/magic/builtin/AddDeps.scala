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

package com.ibm.spark.magic.builtin

import java.io.PrintStream

import com.ibm.spark.magic._
import com.ibm.spark.magic.dependencies.{IncludeDependencyDownloader, IncludeInterpreter, IncludeOutputStream, IncludeSparkContext}
import com.ibm.spark.utils.ArgumentParsingSupport

class AddDeps extends MagicTemplate with IncludeInterpreter
  with IncludeOutputStream with IncludeSparkContext with ArgumentParsingSupport
  with IncludeDependencyDownloader
{

  private lazy val printStream = new PrintStream(outputStream)

  val _transitive =
    parser.accepts("transitive", "retrieve dependencies recursively")
    .withOptionalArg().ofType(classOf[Boolean]).defaultsTo(true)

  /**
   * Execute a magic representing a cell magic.
   * @param code The list of code, separated by newlines
   * @return The output of the magic
   */
  override def executeCell(code: Seq[String]): MagicOutput = {
    code.foreach(executeLine)
    MagicOutput()
  }

  /**
   * Execute a magic representing a line magic.
   * @param code The single line of code
   * @return The output of the magic
   */
  override def executeLine(code: String): MagicOutput = {
    val nonOptionArgs = parseArgs(code)
    dependencyDownloader.setPrintStream(printStream)

    // TODO: require a version or use the most recent if omitted?
    if (nonOptionArgs.size != 3) {
      printHelp(printStream, """%AddDeps my.company artifact-id version""")
      return MagicOutput()
    }

    // get the jars and hold onto the paths at which they reside
    val urls = dependencyDownloader.retrieve(
      nonOptionArgs(0), nonOptionArgs(1), nonOptionArgs(2), _transitive)

    // add the jars to the interpreter and spark context
    interpreter.addJars(urls:_*)
    urls.foreach(url => sparkContext.addJar(url.getPath))

    // TODO: Investigate why %AddJar http://...factorie.jar works without
    //       needing to rebind the SparkContext
    // NOTE: Quick fix to re-enable SparkContext usage, otherwise any code
    //       using the SparkContext (after this) has issues with some sort of
    //       bad type of
    //       org.apache.spark.org.apache.spark.org.apache.spark.SparkContext
    interpreter.bind(
      "sc", "org.apache.spark.SparkContext",
      sparkContext, List("@transient")
    )

    // TODO: report issues, etc, to the user or is the ivy output enough?
    MagicOutput()
  }
}
