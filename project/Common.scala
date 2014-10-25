import sbt._
import Keys._

object Common {
  private val buildOrganization = "com.ibm.spark"
  private val buildVersion      = "0.1.1"
  private val buildScalaVersion = "2.10.4"
  private val buildSbtVersion   = "0.13.5"
  val sparkVersion              = "1.1.0"

  private val artifactPrefix    = "ibm-spark"

  val settings: Seq[Def.Setting[_]] = Seq(
    organization := buildOrganization,
    version := buildVersion,
    scalaVersion := buildScalaVersion,
    sbtVersion := buildSbtVersion,
    // Force artifact name of
    // ibm-spark-<artifact>_<scala version>-<build version>.jar
    artifactName := {
      (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
        artifactPrefix + "-" + artifact.name + "_" + sv.binary + "-" +
          module.revision + "." + artifact.extension
    },
    scalacOptions ++= Seq(
      "-deprecation", "-unchecked", "-feature",
      //"-Xlint", // Scala 2.11.x only
      "-Xfatal-warnings",
      "-Ywarn-all"
    ),
    javacOptions ++= Seq(
      "-Xlint", // Enable all Java-based warnings
      "-Xlint:-path" // Suppress path warnings since we get tons of them
    ),
    // TODO: Investigate why the test options cause a failure with our Java
    //       client code
    //testOptions in Test += Tests.Argument("-oGK"),
    unmanagedResourceDirectories in Compile +=
      (baseDirectory in Build.root).value / "resources/compile",
    unmanagedResourceDirectories in Test +=
      (baseDirectory in Build.root).value / "resources/test",
    publishTo := Some(Resolver.file(
      "file",  new File(Path.userHome.absolutePath + "/.m2/repository"))
    )
  )
}
