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

package org.apache.toree.dependencies

import java.io.{File, PrintStream}
import java.net.{URI, URL}
import java.nio.file.Files
import scala.util.Try
import org.apache.toree.utils.FileUtils

case class DependencyExclude(groupId: String, artifactId: String, version: String);

abstract class DependencyDownloader {
  /**
   * Retrieves the dependency and all of its dependencies as jars.
   *
   * @param groupId The group id associated with the main dependency
   * @param artifactId The id of the dependency artifact
   * @param version The version of the main dependency
   * @param transitive If true, downloads all dependencies of the specified
   *                   dependency
   * @param excludeBaseDependencies If true, will exclude any dependencies
   *                                included in the build of the kernel
   * @param ignoreResolutionErrors If true, ignores any errors on resolving
   *                               dependencies and attempts to download all
   *                               successfully-resolved dependencies
   * @param extraRepositories Additional repositories to use only for this
   *                          dependency
   * @param verbose If true, prints out additional information
   * @param trace If true, prints trace of download process
   *
   * @return The sequence of URIs represented downloaded (even from cache)
   *         dependencies
   */
  def retrieve(
    groupId: String,
    artifactId: String,
    version: String,
    transitive: Boolean = true,
    excludeBaseDependencies: Boolean = true,
    ignoreResolutionErrors: Boolean = true,
    extraRepositories: Seq[(URL, Option[Credentials])] = Nil,
    verbose: Boolean = false,
    trace: Boolean = false,
    configuration: Option[String] = None,
    artifactType: Option[String] = None,
    artifactClassifier: Option[String] = None,
    excludes: Set[(String,String)] = Set.empty
  ): Seq[URI]

  /**
   * Sets the printstream to log to.
   *
   * @param printStream The new print stream to use for output logging
   */
  def setPrintStream(printStream: PrintStream): Unit

  /**
   * Adds the specified resolver url as an additional search option.
   *
   * @param url The url of the repository
   */
  def addMavenRepository(url: URL, credentials: Option[Credentials]): Unit

  /**
   * Remove the specified resolver url from the search options.
   *
   * @param url The url of the repository
   */
  def removeMavenRepository(url: URL): Unit

  /**
   * Returns a list of all repositories used by the downloader.
   *
   * @return The list of repositories as URIs
   */
  def getRepositories: Seq[URI]

  /**
   * Sets the directory where all downloaded jars will be stored.
   *
   * @param directory The directory to use
   *
   * @return True if successfully set directory, otherwise false
   */
  def setDownloadDirectory(directory: File): Boolean

  /**
   * Returns the current directory where dependencies will be downloaded.
   *
   * @return The directory as a string
   */
  def getDownloadDirectory: String


  /**
   * Assigns credentials to the right repository and build tuples
   *
   * @param repository Sequence of repository urls
   * @param credentials Sequence of credential filenames
   * @return
   */
  def resolveRepositoriesAndCredentials(repository: List[String], credentials: List[String]): List[(URL, Option[Credentials])] = {
    val extraRepositories = repository.map(u => (u, Try(new URL(u))))

    // Print error information
    //    extraRepositories.filter(_._2.isFailure).map(_._1)
    //      .foreach(u => printStream.println(s"Ignoring invalid URL $u"))

    // match up credentials with repositories
    val repositories = extraRepositories.flatMap(_._2.toOption)
    val authentication = credentials
      .map(f => new File(f))
      .map(Credentials(_))
      .map(c => (c.host, c)).toMap

    val repositoriesWithCreds = repositories.map(u => (u, authentication.get(u.getHost)))
    repositoriesWithCreds
  }

}

object DependencyDownloader {
  /** Default Maven repository to use with downloaders. */
  val DefaultMavenRepository = new URL("https://repo1.maven.org/maven2")

  /** Default download directory for dependencies. */
  val DefaultDownloadDirectory =
    FileUtils.createManagedTempDirectory("toree-dependency-downloads")
}
