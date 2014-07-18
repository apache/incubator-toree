name := "SparkKernel"

version := "0.1.0"

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

libraryDependencies += "org.apache.spark" %% "spark-core" % "1.0.1" // Apache v2

libraryDependencies += "org.apache.spark" %% "spark-repl" % "1.0.1" // Apache v2

//
// ZEROMQ DEPENDENCIES
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

