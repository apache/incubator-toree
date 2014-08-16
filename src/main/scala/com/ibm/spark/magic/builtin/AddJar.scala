package com.ibm.spark.magic.builtin

import java.io.File
import java.net.URL

import com.ibm.spark.magic.dependencies.{IncludeSparkContext, IncludeInterpreter}
import com.ibm.spark.utils.DownloadSupport

class AddJar
  extends MagicTemplate with IncludeInterpreter with IncludeSparkContext
  with DownloadSupport
{
  // TODO: Figure out where to put this AND a better location as /tmp does not
  //       keep the jars around forever.
  private val JarStorageLocation = "/tmp"

  private val HtmlJarRegex = """.*?\/([^\/]*?)$""".r

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
    val HtmlJarRegex(jarName) = jarRemoteLocation
    val downloadLocation = JarStorageLocation + "/" + jarName

    val jarUrl = downloadFile(
      new URL(jarRemoteLocation),
      new File(downloadLocation).toURI.toURL
    )

    interpreter.addJars(jarUrl)
    sparkContext.addJar(jarUrl.getPath)

    "" // No output needed
  }
}
