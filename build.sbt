name := "SparkKernel"

version := "0.1.0"

scalaVersion := "2.10.4"

//
// ADDITIONAL REPOSITORIES
//

resolvers += "Akka Repository" at "http://repo.akka.io/releases/"

resolvers +=
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

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

