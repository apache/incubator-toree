
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
val sparkVersion = Common.sparkVersion

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion excludeAll   // Apache v2
    ExclusionRule(organization = "org.apache.hadoop"),
  "org.apache.spark" %% "spark-streaming" % sparkVersion,        // Apache v2
  "org.apache.spark" %% "spark-sql" % sparkVersion,              // Apache v2
  "org.apache.spark" %% "spark-mllib" % sparkVersion,            // Apache v2
  "org.apache.spark" %% "spark-graphx" % sparkVersion,           // Apache v2
  "org.apache.spark" %% "spark-repl" % sparkVersion excludeAll   // Apache v2
    ExclusionRule(organization = "org.apache.hadoop")
)

//
// HADOOP DEPENDENCIES
//

libraryDependencies ++= Seq(
  "org.apache.hadoop" % "hadoop-client" % "2.3.0" excludeAll
    ExclusionRule(organization = "javax.servlet")
)

//
// CLI DEPENDENCIES
//

libraryDependencies += "net.sf.jopt-simple" % "jopt-simple" % "4.6" // MIT

//
// MAGIC DEPENDENCIES
//
libraryDependencies ++= Seq(
  "org.apache.ivy" % "ivy" % "2.4.0-rc1" // Apache v2
)

