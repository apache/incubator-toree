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

import scala.util.Properties
import sbtassembly.AssemblyOption

// Must match the Scala version of the Spark distribution built against (see
// APACHE_SPARK_VERSION in the Makefile): scala-compiler and scala-library are
// provided by Spark at runtime, not bundled in the assembly, so compiling with
// a different patch release breaks the interpreter at runtime.
lazy val scala212 = "2.12.17"
lazy val scala213 = "2.13.8"
lazy val defaultScalaVersion = sys.env.get("SCALA_VERSION") match {
  case Some("2.13") => scala213
  case _ => scala212
}

// Version settings
ThisBuild / version := Properties.envOrElse("VERSION", "0.0.0-dev") +
  (if ((ThisBuild / isSnapshot ).value) "-SNAPSHOT" else "")
ThisBuild / isSnapshot := Properties.envOrElse("IS_SNAPSHOT","true").toBoolean
ThisBuild / organization := "org.apache.toree.kernel"
ThisBuild / crossScalaVersions := Seq(scala212, scala213)
ThisBuild / scalaVersion := defaultScalaVersion
ThisBuild / Dependencies.sparkVersion := {
  val envVar = "APACHE_SPARK_VERSION"
  val defaultVersion = "3.4.4"

  Properties.envOrNone(envVar) match {
    case None =>
      sLog.value.info(s"Using default Apache Spark version $defaultVersion!")
      defaultVersion
    case Some(version) =>
      sLog.value.info(s"Using Apache Spark version $version, provided from $envVar")
      version
  }
}

// Compiler settings
ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:reflectiveCalls",
  "-target:jvm-1.8"
)
// Java-based options for compilation (all tasks)
// NOTE: Providing a blank flag causes failures, only uncomment with options
// Compile / javacOptions ++= Seq(""),
// Java-based options for just the compile task
ThisBuild / javacOptions ++= Seq(
  "-Xlint:all",   // Enable all Java-based warnings
  "-Xlint:-path", // Suppress path warnings since we get tons of them
  "-Xlint:-options", // Suppress "options" warnings
  "-Xlint:-processing", // Suppress annotation processing warnings
  "-Werror",       // Treat warnings as errors
  "-source", "1.8",
  "-target", "1.8"
)
// Options provided to forked JVMs through sbt, based on our .jvmopts file
ThisBuild / javaOptions ++= Seq(
  "-Xms1024M", "-Xmx4096M", "-Xss2m", "-XX:MetaspaceSize=1024M",
  "-XX:ReservedCodeCacheSize=256M", "-XX:+HeapDumpOnOutOfMemoryError"
)
// Mockito's inline mock maker self-attaches through the attach API, whose native
// library loads once per JVM; fork tests so each module mocks in its own JVM.
ThisBuild / Test / fork := true
// Add additional test option to show time taken per test
ThisBuild / Test / testOptions += Tests.Argument("-oDF")
// Build-wide dependencies
ThisBuild / resolvers ++= Seq(
  "Apache Snapshots" at "https://repository.apache.org/snapshots/",
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
  "Jitpack" at "https://jitpack.io",
  "bintray-sbt-plugins" at "https://dl.bintray.com/sbt/sbt-plugin-releases"
)
ThisBuild / updateOptions := updateOptions.value.withCachedResolution(true)
ThisBuild / libraryDependencies ++= Seq(
  Dependencies.scalaTest % "test",
  Dependencies.scalaTestMockito % "test",
  Dependencies.mockitoInline % "test",
  Dependencies.jacksonDatabind % "test"
)

// Publish settings
ThisBuild / pgpPassphrase := Some(Properties.envOrElse("MAVEN_GPG_PASSPHRASE","").toArray)
ThisBuild / publishTo := {
  if (isSnapshot.value)
    Some("Apache Staging Repo" at "https://repository.apache.org/content/repositories/snapshots/")
  else
    Some("Apache Staging Repo" at "https://repository.apache.org/content/repositories/staging/")
}
// The incubation disclaimer is read from DISCLAIMER rather than repeated here, so
// the published POMs cannot drift from the file the ASF requires us to ship. It is
// wrapped for readability in the file, so collapse the wrapping for POM metadata.
ThisBuild / description := {
  val disclaimer = IO.read((ThisBuild / baseDirectory).value / "DISCLAIMER")
    .replaceAll("\\s+", " ")
    .trim
  "Apache Toree is a Jupyter Notebook kernel that provides interactive " +
    "applications to connect to and use Apache Spark using Scala language. " +
    disclaimer
}
ThisBuild / licenses := Seq("Apache License, Version 2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / pomExtra := {
  <parent>
    <groupId>org.apache</groupId>
    <artifactId>apache</artifactId>
    <version>39</version>
  </parent>
  <url>https://toree.apache.org/</url>
  <scm>
    <url>https://github.com/apache/incubator-toree</url>
    <connection>scm:git:https://gitbox.apache.org/repos/asf/incubator-toree.git</connection>
    <developerConnection>scm:git:git@github.com:apache/incubator-toree.git</developerConnection>
    <tag>HEAD</tag>
  </scm>
}
ThisBuild / credentials ++= (if ((Path.userHome / ".ivy2" / ".credentials").exists) Seq(Credentials(Path.userHome / ".ivy2" / ".credentials")) else Nil)

// Project structure

/**
  * Settings applied to every module's own (non-assembly) jar so that, unlike the
  * assembly jars (which bundle the dist/toree-legal set separately), each plain
  * published jar carries the project's own top-level LICENSE, NOTICE and DISCLAIMER.
  * Must be scoped to Compile/packageBin directly (not ThisBuild) and appended with
  * ++=, since packageBin's mappings are defined per-project by the JVM plugin and a
  * ThisBuild-scoped assignment is never consulted once that per-project default
  * exists; := would also replace the class/resource mappings instead of adding to them.
  */
lazy val legalFileMappings = Seq(
  Compile / packageBin / mappings ++= Seq(
    file("LICENSE") -> "LICENSE",
    file("NOTICE") -> "NOTICE",
    file("DISCLAIMER") -> "DISCLAIMER"
  )
)

/**
  * Settings applied to the projects that produce an assembly (fat) jar, so that
  * DISCLAIMER, LICENSE and NOTICE from dist/toree-legal land under META-INF instead
  * of the jar root (TOREE-569), matching standard jar conventions. The third-party
  * licenses/ directory from the same source is untouched by this and stays at the
  * jar root, since the ticket only asks to relocate those three files.
  *
  * dist/toree-legal's own on-disk layout is deliberately left alone: `make dist`
  * also copies it directly into the release distribution root, where ASF policy
  * requires LICENSE/NOTICE/DISCLAIMER to stay top-level - a separate concern from
  * how they're laid out inside this jar. So rather than restructure that shared
  * directory, unmanagedResources/excludeFilter drops the three files from the
  * regular (root-level) resource scan, and a resourceGenerator copies them
  * straight into META-INF instead.
  */
lazy val assemblyLegalFiles = Seq(
  Compile / unmanagedResourceDirectories += {
    (ThisBuild / baseDirectory).value / "dist/toree-legal"
  },
  Compile / unmanagedResources / excludeFilter :=
    (Compile / unmanagedResources / excludeFilter).value || "LICENSE" || "NOTICE" || "DISCLAIMER",
  Compile / resourceGenerators += Def.task {
    val legalDir = (ThisBuild / baseDirectory).value / "dist/toree-legal"
    val outDir = (Compile / resourceManaged).value / "META-INF"
    Seq("LICENSE", "NOTICE", "DISCLAIMER").map { name =>
      val target = outDir / name
      IO.copyFile(legalDir / name, target)
      target
    }
  }.taskValue
)

/** Root Toree project. */
lazy val root = (project in file("."))
  .settings(name := "toree")
  .settings(legalFileMappings)
  .settings(assemblyLegalFiles)
  .aggregate(
    macros,protocol,plugins,sparkMonitorPlugin,communication,kernelApi,client,scalaInterpreter,sqlInterpreter,kernel
  )
  .dependsOn(
    macros,protocol,communication,kernelApi,client,scalaInterpreter,sqlInterpreter,kernel
  )

/**
  * Project representing macros in Scala that must be compiled separately from
  * any other project using them.
  */
lazy val macros = (project in file("macros"))
  .settings(name := "toree-macros")
  .settings(legalFileMappings)

/**
  * Project representing the IPython kernel message protocol in Scala. Used
  * by the client and kernel implementations.
  */
lazy val protocol = (project in file("protocol"))
  .settings(name := "toree-protocol")
  .settings(legalFileMappings)
  .dependsOn(macros)

/**
  * Project representing base plugin system for the Toree infrastructure.
  */
lazy val plugins = (project in file("plugins"))
  .settings(name := "toree-plugins")
  .settings(legalFileMappings)
  .dependsOn(macros)

/**
  * Project representing the SparkMonitor plugin for Toree.
  */
lazy val sparkMonitorPlugin = (project in file("spark-monitor-plugin"))
  .settings(name := "toree-spark-monitor-plugin")
  .settings(legalFileMappings)
  // Mirrors the root project's own dist/toree-legal wiring so this project's
  // assembly jar carries the same LICENSE/NOTICE/DISCLAIMER/third-party licenses
  // bundle as toree-assembly (TOREE-570), under META-INF (TOREE-569).
  .settings(assemblyLegalFiles)
  .dependsOn(macros, protocol, plugins, kernel, kernelApi)

/**
  * Project representing forms of communication used as input/output for the
  * client/kernel.
  */
lazy val communication = (project in file("communication"))
  .settings(name := "toree-communication")
  .settings(legalFileMappings)
  .dependsOn(macros, protocol)

/**
* Project representing the kernel-api code used by the Spark Kernel. Others can
* import this to implement their own magics and plugins.
*/
lazy val kernelApi = (project in file("kernel-api"))
  .settings(name := "toree-kernel-api")
  .settings(legalFileMappings)
  .dependsOn(macros, plugins)

/**
* Project representing the client code for connecting to the kernel backend.
*/
lazy val client = (project in file("client"))
  .settings(name := "toree-client")
  .settings(legalFileMappings)
  .dependsOn(macros, protocol, communication)

/**
* Project represents the scala interpreter used by the Spark Kernel.
*/
lazy val scalaInterpreter = (project in file("scala-interpreter"))
  .settings(name := "toree-scala-interpreter")
  .settings(legalFileMappings)
  .dependsOn(plugins, protocol, kernelApi)

/**
* Project represents the SQL interpreter used by the Spark Kernel.
*/
lazy val sqlInterpreter = (project in file("sql-interpreter"))
  .settings(name := "toree-sql-interpreter")
  .settings(legalFileMappings)
  .dependsOn(plugins, protocol, kernelApi, scalaInterpreter)

/**
* Project representing the kernel code for the Spark Kernel backend.
*/
lazy val kernel = (project in file("kernel"))
  .settings(name := "toree-kernel")
  .settings(legalFileMappings)
  .dependsOn(
    macros % "test->test;compile->compile",
    protocol % "test->test;compile->compile",
    communication % "test->test;compile->compile",
    kernelApi % "test->test;compile->compile",
    scalaInterpreter % "test->test;compile->compile",
    sqlInterpreter % "test->test;compile->compile"
  )

// Root project settings
enablePlugins(ScalaUnidocPlugin)
(ScalaUnidoc / unidoc / scalacOptions) ++= Seq(
  "-Ymacro-expand:none",
  "-skip-packages", "org.apache.pekko:scala",
  "-no-link-warnings" // Suppresses problems with Scaladoc @throws links
)

libraryDependencies ++= Dependencies.sparkAll.value

assembly / assemblyShadeRules := Seq(
  ShadeRule.rename("org.clapper.classutil.**" -> "shadeclapper.@0").inAll,
  ShadeRule.rename("org.objectweb.asm.**" -> "shadeasm.@0").inAll
)

assembly / assemblyMergeStrategy := {
  case "module-info.class" => MergeStrategy.discard
  case PathList("META-INF", "versions", "9", "module-info.class") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

assembly / test := {}
assembly / assemblyOption ~= {
  _.withIncludeScala(false)
}
assembly / aggregate := false
// sbt-assembly's default jar name omits the Scala binary version; add it so
// the artifact name matches the Maven convention used by Spark/Hadoop/Flink
// (e.g. toree-assembly_2.12-0.6.0-incubating.jar) instead of colliding across
// cross-builds.
assembly / assemblyJarName := s"toree-assembly_${scalaBinaryVersion.value}-${version.value}.jar"

Global / excludeLintKeys ++= Set(pgpPassphrase, mappings)
