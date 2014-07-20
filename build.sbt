name := "SparkKernel"

version := "0.1.0"

fork := true // http://www.scala-sbt.org/0.13/docs/Running-Project-Code.html#Deserialization+and+class+loading

//exportJars := true

scalaVersion := "2.10.4"

sbtVersion := "0.13.5"

//
// ADDITIONAL REPOSITORIES
//

resolvers += "Akka Repository" at "http://repo.akka.io/releases/"

resolvers +=
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

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
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scala-lang" % "jline" % scalaVersion.value
)

//
// SPARK DEPENDENCIES
//
// NOTE: Currently, version must match deployed Spark cluster version.
//
// TODO: Could kernel dynamically link to Spark library to allow multiple
// TODO: Spark versions? E.g. Spark 1.0.0 and Spark 1.0.1
//

// Apache v2
libraryDependencies +=
  ("org.apache.spark" %% "spark-core" % "1.0.1")

// Apache v2
libraryDependencies +=
  ("org.apache.spark" %% "spark-repl" % "1.0.1")

//
// ZEROMQ DEPENDENCIES
//
// NOTE: Able to run `sbt clean compile run` against standalone cluster with
//       akka-actor, akka-zeromq, and play-json included.
// NOTE: The above run also works with local[*].
//

// NOTE: We are leveraging Akka's ZeroMQ since it is more supported
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.4" // Apache v2

libraryDependencies += "com.typesafe.akka" %% "akka-zeromq" % "2.3.4" // Apache v2

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

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.0" % "test" // Apache v2

libraryDependencies += "org.scalactic" %% "scalactic" % "2.2.0" % "test" // Apache v2

libraryDependencies +=
  "org.scalamock" %% "scalamock-scalatest-support" % "3.0.1" % "test" // MIT


//
// PLUGIN TASK UPDATES
//

net.virtualvoid.sbt.graph.Plugin.graphSettings

