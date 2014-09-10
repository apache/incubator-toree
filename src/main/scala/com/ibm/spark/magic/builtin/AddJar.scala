package com.ibm.spark.magic.builtin

import java.io.{PrintStream, File}
import java.net.URL

import com.ibm.spark.magic.dependencies.{IncludeOutputStream, IncludeSparkContext, IncludeInterpreter}
import com.ibm.spark.utils.DownloadSupport

class AddJar
  extends MagicTemplate with IncludeInterpreter with IncludeSparkContext
  with IncludeOutputStream with DownloadSupport
{
  // TODO: Figure out where to put this AND a better location as /tmp does not
  //       keep the jars around forever.
  private val JarStorageLocation = "/tmp"

  private val HtmlJarRegex = """.*?\/([^\/]*?)$""".r

  // Lazy because the outputStream is not provided at construction
  private lazy val printStream = new PrintStream(outputStream)

  /**
   * Downloads and adds the specified jars to the
   * interpreter/compiler/cluster classpaths.
   * @param code The lines containing the locations of the jars, separated by newline
   */
  override def executeCell(code: Seq[String]): String = {
    code.foreach(executeLine)

    "" // No output needed
  }

  /**
   * Downloads and adds the specified jar to the
   * interpreter/compiler/cluster classpaths.
   * @param code The line containing the location of the jar
   */
  override def executeLine(code: String): String = {
    val jarRemoteLocation = code.trim
    if (jarRemoteLocation.isEmpty)
      return s"%AddJar '$jarRemoteLocation' is invalid!"

    val HtmlJarRegex(jarName) = jarRemoteLocation
    val downloadLocation = JarStorageLocation + "/" + jarName

    // Report beginning of download
    printStream.println(s"Starting download from $jarRemoteLocation")

    val jarUrl = downloadFile(
      new URL(jarRemoteLocation),
      new File(downloadLocation).toURI.toURL
    )

    // Report download finished
    printStream.println(s"Finished download of $jarName")

    interpreter.addJars(jarUrl)
    sparkContext.addJar(jarUrl.getPath)

    "" // No output needed
  }
}
