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

import java.text.SimpleDateFormat
import java.util.Calendar

import com.typesafe.sbt.SbtGhPages.ghpages
import com.typesafe.sbt.SbtSite.site
import sbt.Keys._
import sbt._
import sbtbuildinfo.Plugin._
import sbtunidoc.Plugin.UnidocKeys._
import sbtunidoc.Plugin._
import scoverage.ScoverageSbtPlugin
import xerial.sbt.Pack._
import com.typesafe.sbt.SbtGit.{GitKeys => git}

object Build extends Build with Settings with SubProjects with TestTasks {
  /**
   * Project encapsulating all other child projects.
   */
  lazy val root = addTestTasksToProject(
    Project(
      id = "root",
      base = file("."),
      settings = fullSettings
    ).settings(unidocSettings: _*)
     .settings(site.settings ++ ghpages.settings: _*)
     .settings(
        site.addMappingsToSiteDir(
          mappings in (ScalaUnidoc, packageDoc), "latest/api"
        ),
        git.gitRemoteRepo := "git@github.com:ibm-et/spark-kernel.git"
     )
     .settings(
        scalacOptions in (ScalaUnidoc, unidoc) += "-Ymacro-no-expand",

        // Do not publish the root project (it just serves as an aggregate)
        publishArtifact := false,
        publishLocal := {}
      )
  ).aggregate(
    client, kernel, kernel_api, communication, protocol, macros,
    pyspark_interpreter, scala_interpreter, sparkr_interpreter,
    sql_interpreter
  ).dependsOn(
    client % "test->test",
    kernel % "test->test"
  )
}

/**
 * Contains the settings to be used in all projects.
 */
trait Settings {
  lazy val fullSettings =
    Common.settings ++ Seq(
      ScoverageSbtPlugin.ScoverageKeys.coverageHighlighting := false
    ) ++
    net.virtualvoid.sbt.graph.Plugin.graphSettings
}

/**
 * Contains all projects used in the SparkKernel.
 */
trait SubProjects extends Settings with TestTasks {
  /**
   * Project representing the client code for connecting to the kernel backend.
   */
  lazy val client = addTestTasksToProject(Project(
    id = "client",
    base = file("client"),
    settings = fullSettings
  )) dependsOn(
    macros % "test->test;compile->compile",
    protocol % "test->test;compile->compile",
    communication % "test->test;compile->compile"
  )

  /**
   * Project representing the kernel code for the Spark Kernel backend.
   */
  lazy val kernel = addTestTasksToProject(Project(
    id = "kernel",
    base = file("kernel"),
    settings = fullSettings ++
      packSettings ++ Seq(
        packMain := Map("sparkkernel" -> "com.ibm.spark.SparkKernel")
      )
  )) dependsOn(
    macros % "test->test;compile->compile",
    protocol % "test->test;compile->compile",
    communication % "test->test;compile->compile",
    kernel_api % "test->test;compile->compile",
    pyspark_interpreter % "test->test;compile->compile",
    scala_interpreter % "test->test;compile->compile",
    sparkr_interpreter % "test->test;compile->compile",
    sql_interpreter % "test->test;compile->compile"
  )

  /**
   * Project represents the pyspark interpreter used by the Spark Kernel.
   */
  lazy val pyspark_interpreter = addTestTasksToProject(Project(
    id = "pyspark-interpreter",
    base = file("pyspark-interpreter"),
    settings = fullSettings
  )) dependsOn(
    protocol % "test->test;compile->compile",
    kernel_api % "test->test;compile->compile"
  )

  /**
   * Project represents the scala interpreter used by the Spark Kernel.
   */
  lazy val scala_interpreter = addTestTasksToProject(Project(
    id = "scala-interpreter",
    base = file("scala-interpreter"),
    settings = fullSettings
  )) dependsOn(
    protocol % "test->test;compile->compile",
    kernel_api % "test->test;compile->compile"
  )

  /**
   * Project represents the scala interpreter used by the Spark Kernel.
   */
  lazy val sparkr_interpreter = addTestTasksToProject(Project(
    id = "sparkr-interpreter",
    base = file("sparkr-interpreter"),
    settings = fullSettings
  )) dependsOn(
    protocol % "test->test;compile->compile",
    kernel_api % "test->test;compile->compile"
  )

  /**
   * Project represents the sql interpreter used by the Spark Kernel.
   */
  lazy val sql_interpreter = addTestTasksToProject(Project(
    id = "sql-interpreter",
    base = file("sql-interpreter"),
    settings = fullSettings
  )) dependsOn(
    protocol % "test->test;compile->compile",
    kernel_api % "test->test;compile->compile"
  )

  /**
   * Project representing the kernel-api code used by the Spark Kernel. Others can
   * import this to implement their own magics and plugins.
   */
  lazy val kernel_api = addTestTasksToProject(Project(
    id = "kernel-api",
    base = file("kernel-api"),
    settings = fullSettings ++ packSettings
  )) dependsOn(macros % "test->test;compile->compile")

  /**
   * Required by the sbt-buildinfo plugin. Defines the following:
   * buildDate: Current date of build
   * version: Current kernel version (see buildVersion in Common.scala)
   * scalaVersion: Current Scala version (see buildScalaVersion in Common.scala)
   * sparkVersion: Current Spark version
   */
  lazy val buildSettings = Seq(
    sourceGenerators in Compile <+= buildInfo,
    buildInfoKeys ++= Seq[BuildInfoKey](
      version, scalaVersion,
      "sparkVersion" -> Common.sparkVersion.value,
      "buildDate" -> {
        val simpleDateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss")
        val now = Calendar.getInstance.getTime
        simpleDateFormat.format(now)
      }
    ),
    buildInfoPackage := "com.ibm.spark.kernel"
  )

  /**
   * Project representing forms of communication used as input/output for the
   * client/kernel.
   */
  lazy val communication = addTestTasksToProject(Project(
    id = "communication",
    base = file("communication"),
    settings = fullSettings
  )) dependsOn(
    macros % "test->test;compile->compile",
    protocol % "test->test;compile->compile"
  )

  /**
   * Project representing the IPython kernel message protocol in Scala. Used
   * by the client and kernel implementations.
   */
  lazy val protocol = addTestTasksToProject(Project(
    id = "protocol",
    base = file("protocol"),
    settings = fullSettings ++ buildInfoSettings ++ buildSettings ++ packSettings
  )) dependsOn(macros % "test->test;compile->compile")

  /**
   * Project representing macros in Scala that must be compiled separately from
   * any other project using them.
   */
  lazy val macros = addTestTasksToProject(Project(
    id = "macros",
    base = file("macros"),
    settings = fullSettings
  ))
}

/**
 * Defines custom test tasks to run subsets of our tests.
 *
 * unit:test - runs unit-specific tests
 * integration:test - runs integration (component-to-component) tests
 * system:test - runs system-wide tests
 * scratch:test - runs temporary tests
 */
trait TestTasks {
  def addTestTasksToProject(project: Project): Project =
    project
      .configs( UnitTest )
      .configs( IntegrationTest )
      .configs( SystemTest )
      .configs( ScratchTest )
      .settings( inConfig(UnitTest)(Defaults.testTasks) : _*)
      .settings( inConfig(IntegrationTest)(Defaults.testTasks) : _*)
      .settings( inConfig(SystemTest)(Defaults.testTasks) : _*)
      .settings( inConfig(ScratchTest)(Defaults.testTasks) : _*)
      .settings(
        testOptions in UnitTest := Seq(Tests.Filter(unitFilter)),
        testOptions in IntegrationTest := Seq(Tests.Filter(intFilter)),
        testOptions in SystemTest := Seq(Tests.Filter(sysFilter)),
        testOptions in ScratchTest := Seq(Tests.Filter(scratchFilter))
      )

  def scratchFilter(name: String): Boolean =
    (name endsWith "SpecForScratch") || (name startsWith "scratch.")
  def sysFilter(name: String): Boolean =
    (name endsWith "SpecForSystem") || (name startsWith "system.")
  def intFilter(name: String): Boolean =
    (name endsWith "SpecForIntegration") || (name startsWith "integration.")
  def unitFilter(name: String): Boolean =
    (name endsWith "Spec") && !intFilter(name) &&
      !sysFilter(name) && !scratchFilter(name)

  lazy val UnitTest = config("unit") extend Test
  lazy val IntegrationTest = config("integration") extend Test
  lazy val SystemTest = config("system") extend Test
  lazy val ScratchTest = config("scratch") extend Test
}

