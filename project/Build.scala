import sbt.Keys._
import sbt._
import sbtunidoc.Plugin.UnidocKeys._
import sbtunidoc.Plugin._
import scoverage.ScoverageSbtPlugin._
import xerial.sbt.Pack._

object Build extends Build with Settings with SubProjects with TestTasks {
  /**
   * Project encapsulating all other child projects.
   */
  lazy val root = addTestTasksToProject(
    Project(
      id = "root",
      base = file("."),
      settings = fullSettings
    ).settings(
      scalacOptions in (ScalaUnidoc, unidoc) += "-Ymacro-no-expand"
    )
  ).aggregate(client, kernel, kernel_api, protocol, macros)
}

/**
 * Contains the settings to be used in all projects.
 */
trait Settings {
  lazy val fullSettings =
    Common.settings ++
    instrumentSettings ++ Seq (
      // Enable syntax highlighting (disabled due to bugs in sbt)
      ScoverageKeys.highlighting := true
    ) ++
    net.virtualvoid.sbt.graph.Plugin.graphSettings ++
    unidocSettings
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
    protocol % "test->test;compile->compile"
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
    settings = fullSettings
  )) dependsOn(macros % "test->test;compile->compile")

  /**
   * Project representing the IPython kernel message protocol in Scala. Used
   * by the client and kernel implementations.
   */
  lazy val protocol = addTestTasksToProject(Project(
    id = "protocol",
    base = file("protocol"),
    settings = fullSettings
  ))

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

