package org.apache.toree.dependencies

import java.io.{BufferedInputStream, File, PrintStream}
import java.net.{URI, URL}
import java.nio.file.Files

import coursier.Dependency
import coursier.core.Repository
import coursier.ivy.{IvyXml, IvyRepository}
import coursier.maven.{Pom, MavenRepository}
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

import scalaz.\/
import scalaz.concurrent.Task

/**
 * Represents a dependency downloader for jars that uses Coursier underneath.
 */
class CoursierDependencyDownloader extends DependencyDownloader {
  @volatile private var repositories: Seq[Repository] = Nil
  @volatile private var printStream: PrintStream = System.out
  @volatile private var localDirectory: URI = null

  // Initialization
  setDownloadDirectory(DependencyDownloader.DefaultDownloadDirectory)
  addMavenRepository(DependencyDownloader.DefaultMavenRepository)

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
   *
   * @return The sequence of strings pointing to the retrieved dependency jars
   */
  override def retrieve(
    groupId: String,
    artifactId: String,
    version: String,
    transitive: Boolean,
    excludeBaseDependencies: Boolean,
    ignoreResolutionErrors: Boolean
  ): Seq[URI] = {
    assert(localDirectory != null)
    import coursier._

    // Grab exclusions using base dependencies
    val exclusions: Set[(String, String)] = (if (excludeBaseDependencies) {
      getBaseDependencies.map(_.module).map(m => (m.organization, m.name))
    } else Nil).toSet

    // Mark dependency that we want to download
    val start = Resolution(Set(
      Dependency(
        module = Module(organization = groupId, name = artifactId),
        version = version,
        transitive = transitive,
        exclusions = exclusions // NOTE: Source/Javadoc not downloaded by default
      )
    ))

    printStream.println(s"Marking $groupId:$artifactId:$version for download")

    lazy val defaultBase = new File(localDirectory).getAbsoluteFile

    lazy val downloadLocations = Seq(
      "file:" -> new File(defaultBase, "file"),
      "http://" -> new File(defaultBase, "http"),
      "https://" -> new File(defaultBase, "https")
    )

    // Build list of locations to fetch dependencies
    val fetchLocations = Seq(ivy2Cache(localDirectory)) ++ repositories
    val fetch = Fetch.from(fetchLocations, Cache.fetch(downloadLocations))

    val fetchUris = localDirectory +: repositoriesToURIs(repositories)
    printStream.println("Preparing to fetch from:")
    printStream.println(s"-> ${fetchUris.mkString("\n-> ")}")

    // Verify locations where we will download dependencies
    val resolution = start.process.run(fetch).run

    // Report any resolution errors
    val errors: Seq[(Dependency, Seq[String])] = resolution.errors
    errors.foreach { case (d, e) =>
      printStream.println(s"-> Failed to resolve ${d.module.toString()}:${d.version}")
      e.foreach(s => printStream.println(s"    -> $s"))
    }

    // If resolution errors, do not download
    if (errors.nonEmpty && !ignoreResolutionErrors) return Nil

    // Perform task of downloading dependencies
    val localArtifacts: Seq[FileError \/ File] = Task.gatherUnordered(
      resolution.artifacts.map(a => {
        printStream.println(s"-> Downloading ${a.url}")
        Cache.file(artifact = a, cache = downloadLocations).run
      })
    ).run

    // Print any errors in retrieving dependencies
    localArtifacts.flatMap(_.swap.toOption).map(_.message)
      .foreach(printStream.println)

    // Print success
    val uris = localArtifacts.flatMap(_.toOption).map(_.toURI)
    uris.map(_.getPath).foreach(p => printStream.println(s"-> New file at $p"))

    uris
  }

  /**
   * Adds the specified resolver url as an additional search option.
   *
   * @param url The string representation of the url
   */
  override def addMavenRepository(url: URL): Unit =
    repositories :+= MavenRepository(url.toString)

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
  def getRepositories: Seq[URI] = repositoriesToURIs(repositories)

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
   *
   * @return True if successfully set directory, otherwise false
   */
  override def setDownloadDirectory(directory: File): Boolean = {
    val path = directory.getAbsolutePath
    val cleanPath = if (path.endsWith("/")) path else path + "/"
    val dir = new File(cleanPath)

    if (!dir.isDirectory) return false
    if (!dir.exists() && !dir.mkdirs()) return false

    localDirectory = dir.toURI
    true
  }

  /**
   * Retrieves base dependencies used when building Toree modules.
   *
   * @return The collection of dependencies
   */
  private def getBaseDependencies: Seq[Dependency] = {
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
    val projects = nodes.flatMap(_.right.toOption).map(IvyXml.project)

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
   *
   * @return The resulting URIs
   */
  private def repositoriesToURIs(repositories: Seq[Repository]) = repositories.map {
    case IvyRepository(pattern, _, _, _, _, _, _, _)  => pattern
    case MavenRepository(root, _, _)                  => root
  }.map(new URI(_))

  /** Creates new Ivy2 local repository using base home URI. */
  private def ivy2Local(ivy2HomeUri: URI) = IvyRepository(
    ivy2HomeUri.toString + "local/" +
      "[organisation]/[module]/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)" +
      "[revision]/[type]s/[artifact](-[classifier]).[ext]"
  )

  /** Creates new Ivy2 cache repository using base home URI. */
  private def ivy2Cache(ivy2HomeUri: URI) = IvyRepository(
    ivy2HomeUri.toString + "cache/" +
      "(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[organisation]/[module]/" +
      "[type]s/[artifact]-[revision](-[classifier]).[ext]",
    metadataPatternOpt = Some(
      ivy2HomeUri + "cache/" +
        "(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[organisation]/[module]/" +
        "[type]-[revision](-[classifier]).[ext]"
    ),
    withChecksums = false,
    withSignatures = false,
    dropInfoAttributes = true
  )
}
