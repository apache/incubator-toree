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

lazy val scala212 = "2.12.18"
lazy val scala213 = "2.13.8"
lazy val defaultScalaVersion = sys.env.get("SCALA_VERSION") match {
  case Some("2.12") => scala212
  case _ => scala213
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
  val defaultVersion = "3.5.2"

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
  "-Xlint:-options",
  "-Xlint:-processing",
  "-Werror",       // Treat warnings as errors
  "-source", "1.8",
  "-target", "1.8"
)
// Options provided to forked JVMs through sbt, based on our .jvmopts file
ThisBuild / javaOptions ++= Seq(
  "-Xms1024M", "-Xmx4096M", "-Xss2m", "-XX:MetaspaceSize=1024M",
  "-XX:ReservedCodeCacheSize=256M", "-XX:+HeapDumpOnOutOfMemoryError"
)
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
ThisBuild / pgpPassphrase := Some(Properties.envOrElse("GPG_PASSWORD","").toArray)
ThisBuild / publishTo := {
  if (isSnapshot.value)
    Some("Apache Staging Repo" at "https://repository.apache.org/content/repositories/snapshots/")
  else
    Some("Apache Staging Repo" at "https://repository.apache.org/content/repositories/staging/")
}
ThisBuild / packageBin / mappings := Seq(
  file("LICENSE") -> "LICENSE",
  file("NOTICE") -> "NOTICE"
)
ThisBuild / licenses := Seq("Apache 2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / pomExtra := {
  <parent>
    <groupId>org.apache</groupId>
    <artifactId>apache</artifactId>
    <version>23</version>
  </parent>
  <url>http://toree.incubator.apache.org/</url>
  <scm>
    <url>git@github.com:apache/incubator-toree.git</url>
    <connection>scm:git:git@github.com:apache/incubator-toree.git</connection>
    <developerConnection>
      scm:git:https://gitbox.apache.org/repos/asf/incubator-toree.git
    </developerConnection>
    <tag>HEAD</tag>
  </scm>
}
ThisBuild / credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

// Project structure

/** Root Toree project. */
lazy val root = (project in file("."))
  .settings(name := "toree")
  .aggregate(
    macros,protocol,plugins,communication,kernelApi,client,scalaInterpreter,sqlInterpreter,kernel
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

/**
  * Project representing the IPython kernel message protocol in Scala. Used
  * by the client and kernel implementations.
  */
lazy val protocol = (project in file("protocol"))
  .settings(name := "toree-protocol")
  .dependsOn(macros)

/**
  * Project representing base plugin system for the Toree infrastructure.
  */
lazy val plugins = (project in file("plugins"))
  .settings(name := "toree-plugins")
  .dependsOn(macros)

/**
  * Project representing forms of communication used as input/output for the
  * client/kernel.
  */
lazy val communication = (project in file("communication"))
  .settings(name := "toree-communication")
  .dependsOn(macros, protocol)

/**
* Project representing the kernel-api code used by the Spark Kernel. Others can
* import this to implement their own magics and plugins.
*/
lazy val kernelApi = (project in file("kernel-api"))
  .settings(name := "toree-kernel-api")
  .dependsOn(macros, plugins)

/**
* Project representing the client code for connecting to the kernel backend.
*/
lazy val client = (project in file("client"))
  .settings(name := "toree-client")
  .dependsOn(macros, protocol, communication)

/**
* Project represents the scala interpreter used by the Spark Kernel.
*/
lazy val scalaInterpreter = (project in file("scala-interpreter"))
  .settings(name := "toree-scala-interpreter")
  .dependsOn(plugins, protocol, kernelApi)

/**
* Project represents the SQL interpreter used by the Spark Kernel.
*/
lazy val sqlInterpreter = (project in file("sql-interpreter"))
  .settings(name := "toree-sql-interpreter")
  .dependsOn(plugins, protocol, kernelApi, scalaInterpreter)

/**
* Project representing the kernel code for the Spark Kernel backend.
*/
lazy val kernel = (project in file("kernel"))
  .settings(name := "toree-kernel")
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
Compile / unmanagedResourceDirectories += { baseDirectory.value / "dist/toree-legal" }

assembly / assemblyShadeRules := Seq(
  ShadeRule.rename("org.clapper.classutil.**" -> "shadeclapper.@0").inAll,
  ShadeRule.rename("org.objectweb.asm.**" -> "shadeasm.@0").inAll
)

assembly / assemblyMergeStrategy := {
  case "module-info.class" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

assembly / test := {}
assembly / assemblyOption := (assembly / assemblyOption).value.copy(includeScala = false)
assembly / aggregate := false
