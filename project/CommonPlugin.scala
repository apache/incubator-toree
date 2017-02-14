import sbt._
import sbt.Keys._

object CommonPlugin extends AutoPlugin {

  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  object autoImport {
    lazy val UnitTest = config("unit") extend Test
    lazy val IntegrationTest = config("integration") extend Test
    lazy val SystemTest = config("system") extend Test
  }
  import autoImport._

  override def projectSettings = {
    inConfig(UnitTest)(Defaults.testSettings) ++
    inConfig(IntegrationTest)(Defaults.testSettings) ++
    inConfig(SystemTest)(Defaults.testSettings) ++
    Seq(
      testOptions in UnitTest := Seq(Tests.Filter(unitFilter)),
      testOptions in IntegrationTest := Seq(Tests.Filter(intFilter)),
      testOptions in SystemTest := Seq(Tests.Filter(sysFilter)),
      // Add a global resource directory with compile/ and test/ for resources in all projects
      unmanagedResourceDirectories in Compile ++= Seq(
        (baseDirectory in ThisBuild).value / "resources" / "compile"
      ),
      unmanagedResourceDirectories in Test ++= Seq(
        (baseDirectory in ThisBuild).value / "resources" / "test"
      )
    )
  }

  def sysFilter(name: String): Boolean =
    (name endsWith "SpecForSystem") || (name startsWith "system.")
  def intFilter(name: String): Boolean =
    (name endsWith "SpecForIntegration") || (name startsWith "integration.")
  def unitFilter(name: String): Boolean =
    (name endsWith "Spec") && !intFilter(name) && !sysFilter(name)

}
