package com.ibm.spark.dependencies

import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.apache.ivy.Ivy
import java.io.{PrintStream, File}
import org.apache.ivy.core.module.descriptor.{DefaultExcludeRule, DefaultDependencyDescriptor, DefaultModuleDescriptor}
import org.apache.ivy.core.module.id.{ModuleId, ArtifactId, ModuleRevisionId}
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.core.retrieve.RetrieveOptions
import org.apache.ivy.util.{Message, DefaultMessageLogger}
import scala.io.Source
import org.apache.ivy.plugins.matcher.RegexpPatternMatcher
import java.net.URL

class IvyDependencyDownloader(repositoryUrl: String, baseDirectory: String)
  extends DependencyDownloader(repositoryUrl, baseDirectory)
{
  private val ivySettings = new IvySettings()
  private val resolver = new IBiblioResolver

  resolver.setUsepoms(true)
  resolver.setM2compatible(true)
  resolver.setName("central")

  //adding maven repo resolver
  ivySettings.addResolver(resolver)

  //set to the default resolver
  ivySettings.setDefaultResolver(resolver.getName)

  // Set the destination
  ivySettings.setBaseDir(new File(baseDirectory))
  ivySettings.setDefaultResolutionCacheBasedir(baseDirectory)
  ivySettings.setDefaultRepositoryCacheBasedir(baseDirectory)

  //creates an Ivy instance with settings
  val ivy = Ivy.newInstance(ivySettings)

  override def retrieve(
    groupId: String, artifactId: String, version: String
  ): Seq[URL] = {
    // Start building the ivy.xml file
    val ivyFile = File.createTempFile("ivy", ".xml")
    ivyFile.deleteOnExit()

    val md = DefaultModuleDescriptor.newDefaultInstance(
      ModuleRevisionId.newInstance("com.ibm", "spark-kernel", "working")
    )

    // TODO: add excludes to prevent downloading of spark jars, etc.
    //
    //

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

    // Create our dependency descriptor and its exclusions
    val dd = new DefaultDependencyDescriptor(
      md, ModuleRevisionId.newInstance(groupId, artifactId, version),
      false, false, true
    )
    dd.addExcludeRule("default", sourcesExclusion)
    dd.addExcludeRule("default", javadocExclusion)

    md.addDependency(dd)

    // Creates our ivy configuration file
    XmlModuleDescriptorWriter.write(md, ivyFile)

    // TODO: Remove this (see contents of ivy config)!
    Source.fromFile(ivyFile).getLines().foreach(println)

    // Grab our dependencies (and theirs, etc) recursively
    val resolveOptions = new ResolveOptions()
      .setTransitive(true)
      .setDownload(true)

    // Init resolve report (has what was downloaded, etc)
    val report = ivy.resolve(ivyFile.toURI.toURL, resolveOptions)

    // Get the jar library
    val artifactURLs = report.getAllArtifactsReports
      .map(report => new URL("file:" + report.getLocalFile.getCanonicalPath))

    val moduleDescriptor = report.getModuleDescriptor
    ivy.retrieve(
      moduleDescriptor.getModuleRevisionId,
      baseDirectory + "/[artifact](-[classifier]).[ext]",
      new RetrieveOptions().setConfs(Seq("default").toArray)
    )

    artifactURLs
  }

  /**
   * Uses our printstream in Ivy's LoggingEngine
   * @param printStream the print stream to use
   */
  override def setPrintStream(printStream: PrintStream): Unit = {
    ivy.getLoggerEngine.setDefaultLogger(
      new DefaultMessageLogger(Message.MSG_INFO) {
        override def doEndProgress(msg: String): Unit =
          printStream.println(msg)

        override def doProgress(): Unit =
          printStream.print(".")

        override def log(msg: String, level: Int): Unit =
          if (level <= this.getLevel)
            printStream.println(msg)
      }
    )
  }
}
