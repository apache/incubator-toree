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
import java.net.{URL, URI}
import java.nio.file.{Files, Paths}
import java.util.zip.ZipFile
import org.apache.toree.magic._
import org.apache.toree.magic.builtin.AddJar._
import org.apache.toree.magic.dependencies._
import org.apache.toree.utils.{ArgumentParsingSupport, DownloadSupport, LogLike, FileUtils}
import com.typesafe.config.Config
import org.apache.hadoop.fs.Path
import org.apache.toree.plugins.annotations.Event

object AddJar {
  val HADOOP_FS_SCHEMES = Set("hdfs", "s3", "s3n", "file")

  private var jarDir:Option[String] = None

  def getJarDir(config: Config): String = {
    jarDir.getOrElse({
      jarDir = Some(
        if(config.hasPath("jar_dir") && Files.exists(Paths.get(config.getString("jar_dir")))) {
          config.getString("jar_dir")
        } else {
          FileUtils.createManagedTempDirectory("toree_add_jars").getAbsolutePath
        }
      )
      jarDir.get
    })
  }
}

class AddJar
  extends LineMagic with IncludeInterpreter
  with IncludeOutputStream with DownloadSupport with ArgumentParsingSupport
  with IncludeKernel with IncludePluginManager with IncludeConfig with LogLike
{
  // Option to mark re-downloading of jars
  private val _force =
    parser.accepts("f", "forces re-download of specified jar")

  // Option to mark re-downloading of jars
  private val _magic =
    parser.accepts("magic", "loads jar as a magic extension")

  // Lazy because the outputStream is not provided at construction
  private def printStream = new PrintStream(outputStream)

  /**
    * Validate jar file structure
    *
    * @param jarFile
    * @return boolean value based on validity of jar
    */
  def isValidJar(jarFile: File): Boolean = {
    try {
      val jarZip: ZipFile = new ZipFile(jarFile)
      val entries = jarZip.entries
      if (entries.hasMoreElements) return true else return false
    } catch {
      case _: Throwable => return false
    }
  }


  /**
   * Retrieves file name from a URI.
   *
   * @param location a URI
   * @return The file name of the remote URI, or an empty string if one does not exist
   */
  def getFileFromLocation(location: String): String = {
    val uri = new URI(location)
    val pathParts = uri.getPath.split("/")
    if (pathParts.nonEmpty) {
      pathParts.last
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
  @Event(name = "addjar")
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
        throw new IllegalArgumentException(
          s"The jar file $jarName must end in .jar or .zip."
        )
    }

    val downloadLocation = getJarDir(config) + "/" + jarName

    logger.debug("Downloading jar to %s".format(downloadLocation))

    val fileDownloadLocation = new File(downloadLocation)

    // Check if exists in cache or force applied
    if (_force || !fileDownloadLocation.exists()) {
      // Report beginning of download
      printStream.println(s"Starting download from $jarRemoteLocation")

      val jar = URI.create(jarRemoteLocation)
      if (HADOOP_FS_SCHEMES.contains(jar.getScheme)) {
        val conf = kernel.sparkContext.hadoopConfiguration
        val jarPath = new Path(jarRemoteLocation)
        val fs = jarPath.getFileSystem(conf)
        val destPath = if (downloadLocation.startsWith("file:")) {
          new Path(downloadLocation)
        } else {
          new Path("file:" + downloadLocation)
        }

        fs.copyToLocalFile(
          false /* keep original file */,
          jarPath, destPath,
          true /* don't create checksum files */)
      } else {
        downloadFile(
          new URL(jarRemoteLocation),
          new File(downloadLocation).toURI.toURL
        )
      }

      // Report download finished
      printStream.println(s"Finished download of $jarName")
    } else {
      printStream.println(s"Using cached version of $jarName")
    }

    // validate jar file
    if(! isValidJar(fileDownloadLocation)) {
      throw new IllegalArgumentException(s"Jar '$jarName' is not valid.")
    }

    if (_magic) {
      val plugins = pluginManager.loadPlugins(fileDownloadLocation)
      pluginManager.initializePlugins(plugins)
    } else {
      kernel.addJars(fileDownloadLocation.toURI)
    }
  }
}
