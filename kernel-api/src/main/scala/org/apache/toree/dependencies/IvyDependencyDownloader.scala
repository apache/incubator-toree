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

import org.apache.ivy.Ivy
import org.apache.ivy.core.module.descriptor._
import org.apache.ivy.core.module.id.{ArtifactId, ModuleId, ModuleRevisionId}
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.core.retrieve.RetrieveOptions
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.matcher.RegexpPatternMatcher
import org.apache.ivy.plugins.parser.xml.{XmlModuleDescriptorParser, XmlModuleDescriptorWriter}
import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.apache.ivy.util.{DefaultMessageLogger, Message}
import org.springframework.core.io.support._

/**
 * Represents a dependency downloader for jars that uses Ivy underneath.
 */
class IvyDependencyDownloader(
  val repositoryUrl: String,
  val baseDirectory: String
) extends DependencyDownloader {
  private val ivySettings = new IvySettings()
  private val resolver = new IBiblioResolver

  resolver.setUsepoms(true)
  resolver.setM2compatible(true)
  resolver.setName("central")

  // Add our resolver as the main resolver (IBiblio goes to Maven Central)
  ivySettings.addResolver(resolver)

  // Mark our resolver as the default one to use
  ivySettings.setDefaultResolver(resolver.getName)

  // Set the destination
  ivySettings.setBaseDir(new File(baseDirectory))
  ivySettings.setDefaultResolutionCacheBasedir(baseDirectory)
  ivySettings.setDefaultRepositoryCacheBasedir(baseDirectory)

  //creates an Ivy instance with settings
  val ivy = Ivy.newInstance(ivySettings)

  private def getBaseDependencies: Iterable[DependencyDescriptor] = {
    val xmlModuleDescriptor = XmlModuleDescriptorParser.getInstance()
    val getDependencies = (url: URL) => xmlModuleDescriptor.parseDescriptor(
      new IvySettings(), url, false
    ).getDependencies

    // Find all of the *ivy.xml files on the classpath.
    val ivyFiles = new PathMatchingResourcePatternResolver().getResources(
      "classpath*:**/*ivy.xml"
    )
    val classpathURLs = ivyFiles.map(_.getURI.toURL)

    // Get all of the dependencies from the *ivy.xml files
    val dependencies = classpathURLs.map(getDependencies).flatMap(_.toSeq)

    // Remove duplicates based on artifact name
    val distinctDependencies =
      dependencies.groupBy(_.getDependencyId.getName).map(_._2.head)

    distinctDependencies
  }

  override def retrieve(
    groupId: String,
    artifactId: String,
    version: String,
    transitive: Boolean = true,
    excludeBaseDependencies: Boolean,
    ignoreResolutionErrors: Boolean,
    extraRepositories: Seq[(URL, Option[Credentials])] = Nil,
    verbose: Boolean,
    trace: Boolean,
    configuration: Option[String] = None,
    artifactType: Option[String] = None,
    artifactClassifier: Option[String] = None,
    excludes: Set[(String,String)] = Set.empty
  ): Seq[URI] = {
    // Start building the ivy.xml file
    val ivyFile = File.createTempFile("ivy-custom", ".xml")
    ivyFile.deleteOnExit()

    val md = DefaultModuleDescriptor.newDefaultInstance(
      ModuleRevisionId.newInstance("org.apache.toree", "kernel", "working")
    )

    // Exclude all sources artifacts i.e. artifactId-version-sources.jar
    val moduleId = new ModuleId("*", "*")
    val sourcesArtifactId = new ArtifactId(moduleId, "*", "source", "*")
    val sourcesExclusion = new DefaultExcludeRule(
      sourcesArtifactId, new RegexpPatternMatcher(), null
    )

    // Exclude all javadoc artifacts i.e. artifactId-version-javadoc.jar
    val javadocArtifactId = new ArtifactId(moduleId, "*", "javadoc", "*")
    val javadocExclusion = new DefaultExcludeRule(
      javadocArtifactId, new RegexpPatternMatcher(), null
    )

    // TODO: figure out why this is not excluded. It's in our build.sbt file
    // TODO: and we exclude all deps there. Need to get rid of this hard-code
    val scalaCompilerModuleId = new ModuleId("org.scala-lang", "*")
    val scalaCompilerArtifactId = new ArtifactId(
      scalaCompilerModuleId, "*", "*", "*"
    )
    val scalaCompilerExclusion = new DefaultExcludeRule(
      scalaCompilerArtifactId, new RegexpPatternMatcher(), null
    )


    val scalaLangModuleId = new ModuleId("org.scala-lang.modules", "*")
    val scalaLangArtifactId = new ArtifactId(
      scalaLangModuleId, "*", "*", "*"
    )
    val scalaLangExclusion = new DefaultExcludeRule(
      scalaLangArtifactId, new RegexpPatternMatcher(), null
    )

    // Create our dependency descriptor
    val dependencyDescriptor = new DefaultDependencyDescriptor(
      md, ModuleRevisionId.newInstance(groupId, artifactId, version),
      false, false, true
    )

    md.addDependency(dependencyDescriptor)

    // Add any and all exclusions
    md.addExcludeRule(sourcesExclusion)
    md.addExcludeRule(javadocExclusion)
    md.addExcludeRule(scalaCompilerExclusion)
    md.addExcludeRule(scalaLangExclusion)


    excludes.foreach(x => {
      val moduleId = new ModuleId(x._1,x._2);
      val artifactId = new ArtifactId(moduleId,"*","*","*");
      val exclusion = new DefaultExcludeRule(artifactId, new RegexpPatternMatcher(), null);
      md.addExcludeRule(exclusion);
    })

    // Exclude our base dependencies if marked to do so
    if (excludeBaseDependencies) {
      getBaseDependencies.foreach(dep => {
        val depRevId = dep.getDependencyRevisionId
        val moduleId = new ModuleId(depRevId.getOrganisation, depRevId.getName)
        val artifactId = new ArtifactId(moduleId, "*", "*", "*")
        val excludeRule = new DefaultExcludeRule(
          artifactId, new RegexpPatternMatcher(), null)
        md.addExcludeRule(excludeRule)
      })
    }

    // Creates our ivy configuration file
    XmlModuleDescriptorWriter.write(md, ivyFile)

    // Grab our dependencies (and theirs, etc) recursively
    val resolveOptions = new ResolveOptions()
      .setTransitive(transitive)
      .setDownload(true)

    // Init resolve report (has what was downloaded, etc)
    val report = ivy.resolve(ivyFile.toURI.toURL, resolveOptions)

    // Get the jar libraries
    val artifactURLs = report.getAllArtifactsReports
      .map(report => new URL("file:" + report.getLocalFile.getCanonicalPath))

    val moduleDescriptor = report.getModuleDescriptor
    ivy.retrieve(
      moduleDescriptor.getModuleRevisionId,
      new RetrieveOptions()
        .setConfs(Seq("default").toArray)
        .setDestArtifactPattern(baseDirectory + "/[artifact](-[classifier]).[ext]")
    )

    artifactURLs.map(_.toURI)
  }

  /**
   * Uses our printstream in Ivy's LoggingEngine
   *
   * @param printStream the print stream to use
   */
  override def setPrintStream(printStream: PrintStream): Unit = {
    ivy.getLoggerEngine.setDefaultLogger(
      new DefaultMessageLogger(Message.MSG_INFO) {
        override def doEndProgress(msg: String): Unit = printStream.println(msg)

        override def doProgress(): Unit = printStream.print(".")

        override def log(msg: String, level: Int): Unit =
          if (level <= this.getLevel) printStream.println(msg)
      }
    )
  }

  /**
   * Adds the specified resolver url as an additional search option.
   *
   * @param url The url of the repository
   */
  override def addMavenRepository(url: URL, credentials: Option[Credentials]): Unit = ???

  /**
   * Remove the specified resolver url from the search options.
   *
   * @param url The url of the repository
   */
  override def removeMavenRepository(url: URL): Unit = ???

  /**
   * Returns a list of all repositories used by the downloader.
   *
   * @return The list of repositories as URIs
   */
  override def getRepositories: Seq[URI] = Seq(
    DependencyDownloader.DefaultMavenRepository.toURI
  )

  /**
   * Sets the directory where all downloaded jars will be stored.
   *
   * @param directory The directory to use
   * @return True if successfully set directory, otherwise false
   */
  override def setDownloadDirectory(directory: File): Boolean = false

  /**
   * Returns the current directory where dependencies will be downloaded.
   *
   * @return The directory as a string
   */
  override def getDownloadDirectory: String = baseDirectory
}
