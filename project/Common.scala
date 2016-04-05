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

import org.apache.commons.io.FileUtils
import sbt._
import Keys._
import coursier.Keys._

import scala.util.{Try, Properties}

object Common {
  //  Parameters for publishing to artifact repositories
  val versionNumber             = Properties.envOrElse("VERSION", "0.0.0-dev")
  val snapshot                  = Properties.envOrElse("IS_SNAPSHOT","true").toBoolean

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

  private val buildResolvers = Seq(
    "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
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
    resolvers ++= buildResolvers,

    pomExtra :=
      <parent>
        <groupId>org.apache</groupId>
        <artifactId>apache</artifactId>
        <version>10</version>
      </parent>
      <licenses>
        <license>
          <name>Apache 2</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <url>http://toree.incubator.apache.org/</url>
      <scm>
        <connection>scm:git:git@github.com:apache/incubator-toree.git</connection>
        <developerConnection>scm:git:https://git-wip-us.apache.org/repos/asf/incubator-toree.git</developerConnection>
        <url>scm:git:git@github.com:apache/incubator-toree.git</url>
        <tag>HEAD</tag>
      </scm>,

    mappings in packageBin in Compile += file("LICENSE") -> "LICENSE",
    mappings in packageBin in Compile += file("NOTICE") -> "NOTICE",

    coursierVerbosity := {
      val level = Try(Integer.valueOf(Properties.envOrElse(
        "TOREE_RESOLUTION_VERBOSITY", "1")
      ).toInt).getOrElse(1)

      scala.Console.out.println(
        s"[INFO] Toree Resolution Verbosity Level = $level"
      )

      level
    },

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
      "-language:reflectiveCalls",
      "-target:jvm-1.6"
    ),

    // Java-based options for compilation (all tasks)
    // NOTE: Providing a blank flag causes failures, only uncomment with options
    //javacOptions in Compile ++= Seq(""),

    // Java-based options for just the compile task
    javacOptions in (Compile, compile) ++= Seq(
      "-Xlint:all",   // Enable all Java-based warnings
      "-Xlint:-path", // Suppress path warnings since we get tons of them
      "-Xlint:-options",
      "-Xlint:-processing",
      "-Werror",       // Treat warnings as errors
      "-source", "1.6",
      "-target", "1.6"
    ),

    // Options provided to forked JVMs through sbt, based on our .jvmopts file
    javaOptions ++= Seq(
      "-Xms1024M", "-Xmx4096M", "-Xss2m", "-XX:MaxPermSize=1024M",
      "-XX:ReservedCodeCacheSize=256M", "-XX:+TieredCompilation",
      "-XX:+CMSPermGenSweepingEnabled", "-XX:+CMSClassUnloadingEnabled",
      "-XX:+UseConcMarkSweepGC", "-XX:+HeapDumpOnOutOfMemoryError"
    ),

    // Add additional test option to show time taken per test
    testOptions in Test += Tests.Argument("-oD"),

    // Add a global resource directory with compile/ and test/ for resources
    // in all projects
    unmanagedResourceDirectories in Compile +=
      (baseDirectory in Build.root).value / "resources/compile",
    unmanagedResourceDirectories in Test +=
      (baseDirectory in Build.root).value / "resources/test",

    // Publish Settings
    publishTo := Some("Apache Maven Repo" at "https://repository.apache.org/service/local/staging/deploy/maven2"),
    credentials += Credentials(Path.userHome / ".m2" / ".credentials"),

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
      val inputFile = (crossTarget.value / s"ivy-${version.value}.xml").getAbsoluteFile
      val outputFile =
        ((resourceDirectory in Compile).value / s"${name.value}-ivy.xml").getAbsoluteFile
      s.log.info(s"Copying ${inputFile.getPath} to ${outputFile.getPath}")
      FileUtils.copyFile(inputFile, outputFile)
    }
  )
}
