import xerial.sbt.Pack._
/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
pack <<= pack dependsOn compile

resolvers += "Release Candidates (RC3)" at "https://repository.apache.org/content/repositories/orgapachespark-1065/"

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

// TODO: Mark these as provided and bring them in via the kernel project
//       so users wanting to implement a magic do not bring in Spark itself
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

// Brought in in order to simplify the reading of each project's ivy.xml file
// from the classpath. If we really want we can write our own class and remove
// this dependency but the wheel has already been invented.
libraryDependencies += "org.springframework" % "spring-core" % "4.1.1.RELEASE" // Apache v2
