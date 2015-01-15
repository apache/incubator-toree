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

import java.io.{File, PrintStream}
import java.net.URL

import com.ibm.spark.kernel.protocol.v5.MIMEType
import com.ibm.spark.magic._
import com.ibm.spark.magic.dependencies.{IncludeInterpreter, IncludeOutputStream, IncludeSparkContext}
import com.ibm.spark.utils.{ArgumentParsingSupport, DownloadSupport}

class AddJar
  extends LineMagic with IncludeInterpreter with IncludeSparkContext
  with IncludeOutputStream with DownloadSupport with ArgumentParsingSupport
{
  // TODO: Figure out where to put this AND a better location as /tmp does not
  //       keep the jars around forever.
  private[magic] val JarStorageLocation = "/tmp"

  private val HtmlJarRegex = """.*?\/([^\/]*?)$""".r

  // Option to mark re-downloading of jars
  private val _force =
    parser.accepts("f", "forces re-download of specified jar")
      .withOptionalArg().ofType(classOf[Boolean]).defaultsTo(false)

  // Lazy because the outputStream is not provided at construction
  private lazy val printStream = new PrintStream(outputStream)

  /**
   * Downloads and adds the specified jar to the
   * interpreter/compiler/cluster classpaths.
   *
   * @param code The line containing the location of the jar
   */
  override def execute(code: String): Unit = {
    val nonOptionArgs = parseArgs(code.trim)

    // Check valid arguments
    if (nonOptionArgs.length != 1) {
      printHelp(printStream, """%AddJar <jar_url>""")
      return
    }

    // Check if the jar we want to download is valid
    val jarRemoteLocation = nonOptionArgs(0)
    if (jarRemoteLocation.isEmpty) {
      printHelp(printStream, """%AddJar <jar_url>""")
      return
    }

    // Get the destination of the jar
    val HtmlJarRegex(jarName) = jarRemoteLocation
    val downloadLocation = JarStorageLocation + "/" + jarName
    val fileDownloadLocation = new File(downloadLocation)

    // Check if exists in cache or force applied
    if (_force || !fileDownloadLocation.exists()) {
      // Report beginning of download
      printStream.println(s"Starting download from $jarRemoteLocation")

      downloadFile(
        new URL(jarRemoteLocation),
        new File(downloadLocation).toURI.toURL
      )

      // Report download finished
      printStream.println(s"Finished download of $jarName")
    } else {
      printStream.println(s"Using cached version of $jarName")
    }

    interpreter.addJars(fileDownloadLocation.toURI.toURL)
    sparkContext.addJar(fileDownloadLocation.getCanonicalPath)

    // NOTE: Quick fix to re-enable SparkContext usage, otherwise any code
    //       using the SparkContext (after this) has issues with some sort of
    //       bad type of
    //       org.apache.spark.org.apache.spark.org.apache.spark.SparkContext
    interpreter.doQuietly(
      interpreter.bind(
        "sc", "org.apache.spark.SparkContext",
        sparkContext, List("@transient")
    ))
  }
}
