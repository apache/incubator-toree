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

import java.io.{File, FileInputStream, PrintStream}
import java.net.{URI, URL}
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import coursier.core.Authentication
import coursier._
import coursier.util.{Gather, Task}
import coursier.cache.{ArtifactError, Cache}
import coursier.Dependency
import coursier.core.Repository
import coursier.core.Resolution.ModuleVersion
import coursier.ivy.{IvyRepository, IvyXml}
import coursier.maven.MavenRepository
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

/**
 * Represents a dependency downloader for jars that uses Coursier underneath.
 */
class CoursierDependencyDownloader extends DependencyDownloader {
  @volatile private var repositories: Seq[Repository] = Nil
  @volatile private var printStream: PrintStream = System.out
  @volatile private var localDirectory: URI = null

  // Initialization
  setDownloadDirectory(DependencyDownloader.DefaultDownloadDirectory)
  addMavenRepository(DependencyDownloader.DefaultMavenRepository, None)

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
   * @return The sequence of strings pointing to the retrieved dependency jars
   */
  override def retrieve(
    groupId: String,
    artifactId: String,
    version: String,
    transitive: Boolean,
    excludeBaseDependencies: Boolean,
    ignoreResolutionErrors: Boolean,
    extraRepositories: collection.Seq[(URL, Option[Credentials])] = Nil,
    verbose: Boolean,
    trace: Boolean,
    configuration: Option[String] = None,
    artifactType: Option[String] = None,
    artifactClassifier: Option[String] = None,
    excludes: collection.Set[(String,String)] = collection.Set.empty
  ): collection.Seq[URI] = {
    assert(localDirectory != null)
    import coursier._

    // Grab exclusions using base dependencies (always exclude scala lang)
    val exclusions: collection.Set[(String, String)] = (if (excludeBaseDependencies) {
      getBaseDependencies.map(_.module).map(m => (m.organization.toString, m.name.toString))
    } else Nil).toSet ++ collection.Set(("org.scala-lang".toString, "*".toString), ("org.scala-lang.modules".toString, "*".toString)) ++ excludes

    // Mark dependency that we want to download
    val start = Resolution(Seq(
      Dependency(
        module = Module(organization = Organization(groupId), name = ModuleName(artifactId)),
        version = version
      )
    ))

    printStream.println(s"Marking $groupId:$artifactId:$version for download")

    lazy val defaultBase = new File(localDirectory).getAbsoluteFile

    lazy val downloadLocations = defaultBase

    val allRepositories = extraRepositories.map(x => urlToMavenRepository(x._1, x._2.map(_.authentication))) ++ repositories

    // Build list of locations to fetch dependencies
    val fetchLocations = Seq(ivy2Cache(localDirectory)) ++ allRepositories
    val fetch = ResolutionProcess.fetch(
      fetchLocations,
      Cache.default.fetch
    )

    val fetchUris = localDirectory +: repositoriesToURIs(allRepositories)
    if (verbose) {    
      printStream.println("Preparing to fetch from:")
      printStream.println(s"-> ${fetchUris.mkString("\n-> ")}")
    }
    // Verify locations where we will download dependencies
    val resolution = start.process.run(fetch).unsafeRun()

    // Report any resolution errors
    val errors: collection.Seq[((Module, String), Seq[String])] = resolution.errors
    errors.foreach { case ((dep, msg), e) =>
      printStream.println(s"-> Failed to resolve ${dep.toString()}:${msg}")
      e.foreach(s => printStream.println(s"    -> $s"))
    }

    // If resolution errors, do not download
    if (errors.nonEmpty && !ignoreResolutionErrors) return Nil

    // Perform task of downloading dependencies
    val localArtifacts: collection.Seq[Either[ArtifactError, File]] = Gather[Task].gather(
      resolution.artifacts().map(Cache.default.file(_).run)
    ).unsafeRun()

    // Print any errors in retrieving dependencies
    localArtifacts.flatMap(_.swap.toOption).map(_.message)
      .foreach(printStream.println)

    // Print success
    val uris = localArtifacts.flatMap(_.toOption).map(_.toURI)

    if (verbose) uris.map(_.getPath).foreach(p => printStream.println(s"-> New file at $p"))
    
    printStream.println("Obtained " + uris.size + " files")
    
    uris
  }

  /**
   * Adds the specified resolver url as an additional search option.
   *
   * @param url The string representation of the url
   */
  override def addMavenRepository(url: URL, credentials: Option[Credentials]): Unit =
    repositories :+= urlToMavenRepository(url, credentials.map(_.authentication))

  private def urlToMavenRepository(url: URL, credentials: Option[Authentication]) = MavenRepository(url.toString, authentication = credentials)

  /**
   * Remove the specified resolver url from the search options.
   *
   * @param url The url of the repository
   */
  override def removeMavenRepository(url: URL): Unit = {
    repositories = repositories.filterNot {
      case maven: MavenRepository => url.toString == maven.root
      case _                      => false
    }
  }

  /**
   * Sets the printstream to log to.
   *
   * @param printStream The new print stream to use for output logging
   */
  override def setPrintStream(printStream: PrintStream): Unit =
    this.printStream = printStream

  /**
   * Returns a list of all repositories used by the downloader.
   *
   * @return The list of repositories as URIs
   */
  def getRepositories: collection.Seq[URI] = repositoriesToURIs(repositories)

  /**
   * Returns the current directory where dependencies will be downloaded.
   *
   * @return The directory as a string
   */
  override def getDownloadDirectory: String =
    new File(localDirectory).getAbsolutePath

  /**
   * Sets the directory where all downloaded jars will be stored.
   *
   * @param directory The directory to use
   * @return True if successfully set directory, otherwise false
   */
  override def setDownloadDirectory(directory: File): Boolean = {
    val path = directory.getAbsolutePath
    val cleanPath = if (path.endsWith("/")) path else path + "/"
    val dir = new File(cleanPath)

    if (!dir.exists() && !dir.mkdirs()) return false
    if (!dir.isDirectory) return false

    localDirectory = dir.toURI
    true
  }

  /**
   * Retrieves base dependencies used when building Toree modules.
   *
   * @return The collection of dependencies
   */
  private def getBaseDependencies: collection.Seq[Dependency] = {
    import coursier.core.compatibility.xmlParse

    // Find all of the *ivy.xml files on the classpath.
    val ivyFiles = new PathMatchingResourcePatternResolver().getResources(
      "classpath*:**/*ivy.xml"
    )
    val streams = ivyFiles.map(_.getInputStream)
    val contents = streams.map(scala.io.Source.fromInputStream).map(_.getLines())
    val nodes = contents.map(c => xmlParse(c.mkString("\n")))

    // Report any errors reading XML
    nodes.flatMap(_.left.toOption).foreach(s => printStream.println(s"Error: $s"))

    // Grab Ivy XML projects
    val projects = nodes.flatMap(_.toOption).map(IvyXml.project)

    // Report any errors parsing Ivy XML
    projects.flatMap(_.swap.toOption).foreach(s => printStream.println(s"Error: $s"))

    // Grab dependencies from projects
    val dependencies = projects.flatMap(_.toOption).flatMap(_.dependencies.map(_._2))

    // Return unique dependencies
    dependencies.distinct
  }

  /**
   * Converts the provide repositories to their URI representations.
   *
   * @param repositories The repositories to convert
   * @return The resulting URIs
   */
  private def repositoriesToURIs(repositories: collection.Seq[Repository]) =
    repositories.map {
      case ivy: IvyRepository => ivy.pattern.string
      case maven: MavenRepository => maven.root
    }.map(s => Try(new URI(s))).filter(_.isSuccess).map(_.get)

  /** Creates new Ivy2 local repository using base home URI. */
  private def ivy2Local(ivy2HomeUri: URI) = IvyRepository.parse(
    ivy2HomeUri.toString + "local/" +
      "[organisation]/[module]/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)" +
      "[revision]/[type]s/[artifact](-[classifier]).[ext]"
  ).toOption.get

  /** Creates new Ivy2 cache repository using base home URI. */
  private def ivy2Cache(ivy2HomeUri: URI) = IvyRepository.parse(
    ivy2HomeUri.toString + "cache/" +
      "(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[organisation]/[module]/" +
      "[type]s/[artifact]-[revision](-[classifier]).[ext]",
    metadataPatternOpt = Some(
      ivy2HomeUri.toString + "cache/" +
        "(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[organisation]/[module]/" +
        "[type]-[revision](-[classifier]).[ext]"
    ),
    withChecksums = false,
    withSignatures = false,
    dropInfoAttributes = true
  ).toOption.get
}


sealed abstract class Credentials extends Product with Serializable {
  def user: String
  def password: String
  def host: String

  def authentication: Authentication =
    Authentication(user, password)
}

object Credentials {

  case class FromFile(file: File) extends Credentials {

    private lazy val props = {
      val p = new Properties()
      p.load(new FileInputStream(file))
      p
    }

    private def findKey(keys: collection.Seq[String]) = keys
      .iterator
      .map(props.getProperty)
      .filter(_ != null)
      .to(LazyList)
      .headOption
      .getOrElse {
        throw new NoSuchElementException(s"${keys.head} key in $file")
      }

    lazy val user: String = findKey(FromFile.fileUserKeys)
    lazy val password: String = findKey(FromFile.filePasswordKeys)
    lazy val host: String = findKey(FromFile.fileHostKeys)
  }

  object FromFile {
    // from sbt.Credentials
    private val fileUserKeys = Seq("user", "user.name", "username")
    private val filePasswordKeys = Seq("password", "pwd", "pass", "passwd")
    private val fileHostKeys = Seq("host", "host.name", "hostname", "domain")
  }


  def apply(file: File): Credentials =
    FromFile(file)

}
