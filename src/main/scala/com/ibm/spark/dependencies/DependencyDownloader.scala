package com.ibm.spark.dependencies

import java.io.PrintStream
import java.net.URL

abstract class DependencyDownloader(repositoryUrl: String, baseDir: String) {

  /**
   * Retrieves the dependency and all of its dependencies as jars.
   *
   * @param groupId The group id associated with the main dependency
   * @param artifactId The id of the dependency artifact
   * @param version The version of the main dependency
   *
   * @return The sequence of strings pointing to the retrieved dependency jars
   */
  def retrieve(groupId: String, artifactId: String, version: String): Seq[URL]

  /**
   * Sets the printstream to log to.
   *
   * @param printStream The new print stream to use for output logging
   */
  def setPrintStream(printStream: PrintStream): Unit

}
