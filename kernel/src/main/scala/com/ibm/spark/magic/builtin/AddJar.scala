/*
 * Copyright 2015 IBM Corp.
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
import java.nio.file.{Files, Paths}

import com.ibm.spark.magic._
import com.ibm.spark.magic.builtin.AddJar._
import com.ibm.spark.magic.dependencies._
import com.ibm.spark.utils.{ArgumentParsingSupport, DownloadSupport, LogLike}
import com.typesafe.config.Config

object AddJar {

  private var jarDir:Option[String] = None
  def getJarDir(config: Config): String = {
    jarDir.getOrElse({
      jarDir = Some(
        if(config.hasPath("jar_dir") && Files.exists(Paths.get(config.getString("jar_dir")))) {
          config.getString("jar_dir")
        } else {
          Files.createTempDirectory("spark_kernel_add_jars").toFile.getAbsolutePath
        }
      )
      jarDir.get
    })
  }
}

class AddJar
  extends LineMagic with IncludeInterpreter with IncludeSparkContext
  with IncludeOutputStream with DownloadSupport with ArgumentParsingSupport
  with IncludeKernel with IncludeMagicLoader with IncludeConfig with LogLike
{
  // Option to mark re-downloading of jars
  private val _force =
    parser.accepts("f", "forces re-download of specified jar")

  // Option to mark re-downloading of jars
  private val _magic =
    parser.accepts("magic", "loads jar as a magic extension")

  // Lazy because the outputStream is not provided at construction
  private lazy val printStream = new PrintStream(outputStream)

  /**
   * Retrieves file name from URL.
   *
   * @param location The remote location (URL) 
   * @return The name of the remote URL, or an empty string if one does not exist
   */
  def getFileFromLocation(location: String): String = {
    val url = new URL(location)
    val file = url.getFile.split("/")
    if (file.length > 0) {
        file.last
    } else {
        ""
    }
  }

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
    val jarName = getFileFromLocation(jarRemoteLocation)

    // Ensure the URL actually contains a jar or zip file
    if (!jarName.endsWith(".jar") && !jarName.endsWith(".zip")) {
        throw new IllegalArgumentException(s"The jar file $jarName must end in .jar or .zip.")
    }

    val downloadLocation = getJarDir(config) + "/" + jarName

    logger.debug( "Downloading jar to %s".format(downloadLocation) )

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


    if (_magic)
    {

      magicLoader.addJar(fileDownloadLocation.toURI.toURL)

    }
    else
    {
      interpreter.addJars(fileDownloadLocation.toURI.toURL)
      sparkContext.addJar(fileDownloadLocation.getCanonicalPath)

    }
  }
}
