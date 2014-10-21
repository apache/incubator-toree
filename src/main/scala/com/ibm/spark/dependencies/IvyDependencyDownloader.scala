package com.ibm.spark.dependencies

import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.apache.ivy.Ivy
import java.io.{PrintStream, File}
import org.apache.ivy.core.module.descriptor.{DependencyDescriptor, DefaultExcludeRule, DefaultDependencyDescriptor, DefaultModuleDescriptor}
import org.apache.ivy.core.module.id.{ModuleId, ArtifactId, ModuleRevisionId}
import org.apache.ivy.plugins.parser.xml.{XmlModuleDescriptorParser, XmlModuleDescriptorWriter}
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.core.retrieve.RetrieveOptions
import org.apache.ivy.util.{Message, DefaultMessageLogger}
import scala.io.Source
import org.apache.ivy.plugins.matcher.{ExactPatternMatcher, RegexpPatternMatcher}
import java.net.URL
import com.ibm.spark.SparkKernel

import collection.JavaConverters._

class IvyDependencyDownloader(repositoryUrl: String, baseDirectory: String)
  extends DependencyDownloader(repositoryUrl, baseDirectory)
{
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

  private def getBaseDependencies: Seq[DependencyDescriptor] = {
    val baseIvyFile = SparkKernel.getClass.getResource("/ivy.xml")
    val baseMD = XmlModuleDescriptorParser.getInstance().parseDescriptor(
      new IvySettings(), baseIvyFile.toURI.toURL, false)
    baseMD.getDependencies
  }

  override def retrieve(
    groupId: String, artifactId: String, version: String, transitive: Boolean
  ): Seq[URL] = {
    // Start building the ivy.xml file
    val ivyFile = File.createTempFile("ivy-custom", ".xml")
    ivyFile.deleteOnExit()

    val md = DefaultModuleDescriptor.newDefaultInstance(
      ModuleRevisionId.newInstance("com.ibm", "spark-kernel", "working")
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

    getBaseDependencies.foreach(dep => {
      val depRevId = dep.getDependencyRevisionId
      val moduleId = new ModuleId(depRevId.getOrganisation, depRevId.getName)
      val artifactId = new ArtifactId(moduleId, "*", "*", "*")
      val excludeRule = new DefaultExcludeRule(
        artifactId, new RegexpPatternMatcher(), null)
      md.addExcludeRule(excludeRule)
    })

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
