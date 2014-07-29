import sbt._
import Keys._

object TestTasks extends Build {
  lazy val root =
    Project("root", file("."))
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

  lazy val UnitTest = config("unit") extend(Test)
  lazy val IntegrationTest = config("integration") extend(Test)
  lazy val SystemTest = config("system") extend(Test)
  lazy val ScratchTest = config("scratch") extend(Test)
}
