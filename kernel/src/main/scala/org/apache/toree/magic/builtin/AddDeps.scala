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

package org.apache.toree.magic.builtin

import java.io.PrintStream

import org.apache.toree.magic._
import org.apache.toree.magic.dependencies._
import org.apache.toree.utils.ArgumentParsingSupport

class AddDeps extends LineMagic with IncludeInterpreter
  with IncludeOutputStream with IncludeSparkContext with ArgumentParsingSupport
  with IncludeDependencyDownloader with IncludeKernel
{

  private lazy val printStream = new PrintStream(outputStream)

  val _transitive =
    parser.accepts("transitive", "retrieve dependencies recursively")

  /**
   * Execute a magic representing a line magic.
   * @param code The single line of code
   * @return The output of the magic
   */
  override def execute(code: String): Unit = {
    val nonOptionArgs = parseArgs(code)
    dependencyDownloader.setPrintStream(printStream)

    // TODO: require a version or use the most recent if omitted?
    if (nonOptionArgs.size == 3) {
      // get the jars and hold onto the paths at which they reside
      val urls = dependencyDownloader.retrieve(
        nonOptionArgs(0), nonOptionArgs(1), nonOptionArgs(2), _transitive)

      // add the jars to the interpreter and spark context
      interpreter.addJars(urls:_*)
      urls.foreach(url => sparkContext.addJar(url.getPath))
    } else {
      printHelp(printStream, """%AddDeps my.company artifact-id version""")
    }
  }
}
