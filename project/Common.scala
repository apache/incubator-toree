/*
 * Copyright 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.io.FileUtils
import sbt._
import Keys._

import scala.util.Properties

object Common {
  //  Parameters for publishing to artifact repositories
  val versionNumber             = Properties.envOrElse("VERSION", "0.0.0-dev")
  val snapshot                  = Properties.envOrElse("IS_SNAPSHOT","true").toBoolean
  val repoPort                  = Properties.envOrElse("REPO_PORT","")
  val repoHost                  = Properties.envOrElse("REPO_HOST","")
  val repoUsername              = Properties.envOrElse("REPO_USERNAME","")
  val repoPassword              = Properties.envOrElse("REPO_PASSWORD","")
  val repoEndpoint              = Properties.envOrElse("REPO_ENDPOINT", if(snapshot) "/nexus/content/repositories/snapshots/" else "/nexus/content/repositories/releases/")
  val repoUrl                   = Properties.envOrElse("REPO_URL", s"http://${repoHost}:${repoPort}${repoEndpoint}")


  private val buildOrganization = "org.apache.toree"
  private val buildVersion      =
    if (snapshot) s"$versionNumber-SNAPSHOT"
    else versionNumber
  private val buildScalaVersion = "2.10.4"


  // Global dependencies provided to all projects
  private var buildLibraryDependencies = Seq(

    // Needed to force consistent typesafe config with play json and spark
    "com.typesafe" % "config" % "1.2.1",
    "org.slf4j" % "slf4j-log4j12" % "1.7.5" % "test",
    "log4j" % "log4j" % "1.2.17" % "test",
    "org.scalatest" %% "scalatest" % "2.2.0" % "test", // Apache v2
    "org.scalactic" %% "scalactic" % "2.2.0" % "test", // Apache v2
    "org.mockito" % "mockito-all" % "1.9.5" % "test"   // MIT
  )

  // The prefix used for our custom artifact names
  private val artifactPrefix = "ibm-spark"
  lazy val sparkVersion = {
    val sparkEnvironmentVariable = "APACHE_SPARK_VERSION"
    val defaultSparkVersion = "1.5.1"

    val _sparkVersion = Properties.envOrNone(sparkEnvironmentVariable)

    if (_sparkVersion.isEmpty) {
      scala.Console.out.println(
        s"""
           |[INFO] Using default Apache Spark $defaultSparkVersion!
           """.stripMargin.trim.replace('\n', ' '))
      defaultSparkVersion
    } else {
      val version = _sparkVersion.get
      scala.Console.out.println(
        s"""
           |[INFO] Using Apache Spark $version provided from
                                                |$sparkEnvironmentVariable!
           """.stripMargin.trim.replace('\n', ' '))
      version
    }
  }

  val settings: Seq[Def.Setting[_]] = Seq(
    organization := buildOrganization,
    version := buildVersion,
    scalaVersion := buildScalaVersion,
    libraryDependencies ++= buildLibraryDependencies,
    isSnapshot := snapshot,

    scalacOptions in (Compile, doc) ++= Seq(
      // Ignore packages (for Scaladoc) not from our project
      "-skip-packages", Seq(
        "akka",
        "scala"
      ).mkString(":")
    ),

    // Scala-based options for compilation
    scalacOptions ++= Seq(
      "-deprecation", "-unchecked", "-feature",
      //"-Xlint", // Scala 2.11.x only
      "-Xfatal-warnings",
      "-Ywarn-all",
      "-language:reflectiveCalls"
    ),

    // Java-based options for compilation (all tasks)
    javacOptions in Compile ++= Seq(""),

    // Java-based options for just the compile task
    javacOptions in (Compile, compile) ++= Seq(
      "-Xlint:all",   // Enable all Java-based warnings
      "-Xlint:-path", // Suppress path warnings since we get tons of them
      "-Werror"       // Treat warnings as errors
    ),

    // Add additional test option to show time taken per test
    testOptions in Test += Tests.Argument("-oD"),

    // Add a global resource directory with compile/ and test/ for resources
    // in all projects
    unmanagedResourceDirectories in Compile +=
      (baseDirectory in Build.root).value / "resources/compile",
    unmanagedResourceDirectories in Test +=
      (baseDirectory in Build.root).value / "resources/test",

    publishTo := Some("Spark Kernel Nexus Repo" at repoUrl),

    credentials += Credentials("Sonatype Nexus Repository Manager", repoHost, repoUsername, repoPassword),

    // Change destination of local delivery (building ivy.xml) to have *-ivy.xml
    deliverLocalConfiguration := {
      val newDestinationPath = crossTarget.value / s"${name.value}-ivy.xml"
      val dlc = deliverLocalConfiguration.value
      new DeliverConfiguration(
        newDestinationPath.absolutePath, dlc.status,
        dlc.configurations, dlc.logging)
    },

    // Add rebuild ivy xml to the following tasks
    compile <<= (compile in Compile) dependsOn (rebuildIvyXml dependsOn deliverLocal)
  ) ++ rebuildIvyXmlSettings // Include our rebuild ivy xml settings


  buildLibraryDependencies ++= Seq( "org.apache.spark" %% "spark-core" % sparkVersion  % "provided" excludeAll( // Apache v2

    // Exclude netty (org.jboss.netty is for 3.2.2.Final only)
    ExclusionRule(
      organization = "org.jboss.netty",
      name = "netty"
    )
    ),
    "org.apache.spark" %% "spark-streaming" % sparkVersion % "provided",
    "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
    "org.apache.spark" %% "spark-mllib" % sparkVersion % "provided",
    "org.apache.spark" %% "spark-graphx" % sparkVersion % "provided",
    "org.apache.spark" %% "spark-repl" % sparkVersion  % "provided"
  )

  // ==========================================================================
  // = REBUILD IVY XML SETTINGS BELOW
  // ==========================================================================

  lazy val rebuildIvyXml = TaskKey[Unit](
    "rebuild-ivy-xml",
    "Rebuilds the ivy xml using deliver-local and copies it to src " +
      "resource directories"
  )

  // TODO: Figure out how to retrieve the configuration being used to avoid
  //       this duplication
  lazy val rebuildIvyXmlSettings = Seq(
    rebuildIvyXml := {
      val s: TaskStreams = streams.value
      val inputFile = (crossTarget.value / s"${name.value}-ivy.xml").getAbsoluteFile
      val outputFile =
        ((resourceDirectory in Compile).value / s"${name.value}-ivy.xml").getAbsoluteFile
      s.log.info(s"Copying ${inputFile.getPath} to ${outputFile.getPath}")
      FileUtils.copyFile(inputFile, outputFile)
    }
  )
}
