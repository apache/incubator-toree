package com.ibm.spark.magic.builtin

import java.io.{File, PrintStream}
import java.net.URL

import com.ibm.spark.kernel.protocol.v5.MIMEType
import com.ibm.spark.magic.{MagicTemplate, MagicOutput}
import com.ibm.spark.magic.dependencies.{IncludeInterpreter, IncludeOutputStream, IncludeSparkContext}
import com.ibm.spark.utils.{ArgumentParsingSupport, DownloadSupport}

import scala.io.Source

class AddJar
  extends MagicTemplate with IncludeInterpreter with IncludeSparkContext
  with IncludeOutputStream with DownloadSupport with ArgumentParsingSupport
{
  // TODO: Figure out where to put this AND a better location as /tmp does not
  //       keep the jars around forever.
  private[builtin] val JarStorageLocation = "/tmp"

  private val HtmlJarRegex = """.*?\/([^\/]*?)$""".r

  // Option to mark redownloading of jars
  private val _force =
    parser.accepts("force", "forces redownload of specified jar")

  // Lazy because the outputStream is not provided at construction
  private lazy val printStream = new PrintStream(outputStream)

  /**
   * Downloads and adds the specified jars to the
   * interpreter/compiler/cluster classpaths.
   *
   * @param code The lines containing the locations of the jars,
   *             separated by newline
   */
  override def executeCell(code: Seq[String]): MagicOutput = {
    code.foreach(executeLine)

    MagicOutput() // No output needed
  }

  /**
   * Downloads and adds the specified jar to the
   * interpreter/compiler/cluster classpaths.
   *
   * @param code The line containing the location of the jar
   */
  override def executeLine(code: String): MagicOutput = {
    val nonOptionArgs = parseArgs(code.trim)

    // Check valid arguments
    if (nonOptionArgs.length != 1) {
      printHelp(printStream, """%AddJar <jar_url>""")
      return MagicOutput()
    }

    // Check if the jar we want to download is valid
    val jarRemoteLocation = nonOptionArgs(0)
    if (jarRemoteLocation.isEmpty)
      return MagicOutput(
        MIMEType.PlainText ->
          s"%AddJar '$jarRemoteLocation' is invalid!"
      )

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

    MagicOutput() // No output needed
  }
}
