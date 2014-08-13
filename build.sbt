import de.johoop.testngplugin.TestNGPlugin._

name := "SparkKernel"

version := "0.1.0"

fork := true // http://www.scala-sbt.org/0.13/docs/Running-Project-Code.html#Deserialization+and+class+loading

//exportJars := true

scalaVersion := "2.10.4"

sbtVersion := "0.13.5"

scalacOptions ++= Seq( "-deprecation", "-unchecked", "-feature", "-Xlint" )

//
// CUSTOM TASKS
//

lazy val kill = taskKey[Unit]("Executing the shell script.")

kill := {
  "sh terminate_spark_kernels.sh" !
}


//
// ADDITIONAL REPOSITORIES
//

resolvers += "Akka Repository" at "http://repo.akka.io/releases/"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

//
// SCALA INTERPRETER DEPENDENCIES
//

managedScalaInstance := false

// Add the configuration for the dependencies on Scala tool jars
// You can also use a manually constructed configuration like:
//   config("scala-tool").hide
ivyConfigurations += Configurations.ScalaTool

// Add the usual dependency on the library as well on the compiler in the
//  'scala-tool' configuration
libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-library" % scalaVersion.value,
  "org.scala-lang" % "scala-compiler" % scalaVersion.value % "scala-tool",
  "org.scala-lang" % "scala-reflect" % scalaVersion.value
)

//
// SPARK DEPENDENCIES
//
// NOTE: Currently, version must match deployed Spark cluster version.
//
// TODO: Could kernel dynamically link to Spark library to allow multiple
// TODO: Spark versions? E.g. Spark 1.0.0 and Spark 1.0.1
//

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.0.0", // Apache v2
  "org.apache.spark" %% "spark-streaming" % "1.0.0", // Apache v2
  "org.apache.spark" %% "spark-repl" % "1.0.0" // Apache v2
)

//
// AKKA DEPENDENCIES (from Spark project)
//
// NOTE: Able to run `sbt clean compile run` against standalone cluster with
//       akka-actor, akka-zeromq, and play-json included.
// NOTE: The above run also works with local[*].
//
libraryDependencies += "org.spark-project.akka" %% "akka-zeromq" % "2.2.3-shaded-protobuf" // Apache v2

//
// JSON DEPENDENCIES
//

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.1" // Apache v2

//
// CLI DEPENDENCIES
//

libraryDependencies += "net.sf.jopt-simple" % "jopt-simple" % "4.6" // MIT

//
// TEST DEPENDENCIES
//
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.0" % "test", // Apache v2
  "org.scalactic" %% "scalactic" % "2.2.0" % "test", // Apache v2
  "org.mockito" % "mockito-all" % "1.9.5" % "test",// MIT
  "org.spark-project.akka" %% "akka-testkit" % "2.2.3-shaded-protobuf" % "test" // MIT
)


//  Java Test Dependencies
libraryDependencies ++= Seq(
  "org.testng" % "testng" % "6.8.5" % "test",
  "org.mockito" %"mockito-all" % "1.9.5" % "test",
  "org.easytesting" % "fest-assert" % "1.4" % "test",
  "org.fluentlenium" % "fluentlenium-testng" % "0.9.0" % "test",
  "org.fluentlenium" % "fluentlenium-festassert" % "0.9.0" % "test",
  "org.fluentlenium" % "fluentlenium-core" % "0.9.0" % "test"
)

//
// PLUGIN TASK UPDATES
//

net.virtualvoid.sbt.graph.Plugin.graphSettings

instrumentSettings

ScoverageKeys.highlighting := true

packSettings

packMain := Map("sparkkernel" -> "com.ibm.spark.SparkKernel")

testNGSettings
