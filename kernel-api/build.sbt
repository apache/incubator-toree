import xerial.sbt.Pack._
import Common._
/*
 * Copyright 2015 IBM Corp.
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

// TODO: Mark these as provided and bring them in via the kernel project
//       so users wanting to implement a magic do not bring in Spark itself
//libraryDependencies ++= Seq(
//  "org.apache.spark" %% "spark-core" % sparkVersion.value excludeAll( // Apache v2
//    ExclusionRule(organization = "org.apache.hadoop"),
//
//    // Exclude netty (org.jboss.netty is for 3.2.2.Final only)
//    ExclusionRule(
//      organization = "org.jboss.netty",
//      name = "netty"
//    )
//  ),
//  "org.apache.spark" %% "spark-streaming" % sparkVersion.value,      // Apache v2
//  "org.apache.spark" %% "spark-sql" % sparkVersion.value,            // Apache v2
//  "org.apache.spark" %% "spark-mllib" % sparkVersion.value,          // Apache v2
//  "org.apache.spark" %% "spark-graphx" % sparkVersion.value,         // Apache v2
//  "org.apache.spark" %% "spark-repl" % sparkVersion.value excludeAll // Apache v2
//    ExclusionRule(organization = "org.apache.hadoop")
//)

//
// HADOOP DEPENDENCIES
//
//libraryDependencies ++= Seq(
//  "org.apache.hadoop" % "hadoop-client" % hadoopVersion.value excludeAll
//    ExclusionRule(organization = "javax.servlet")
//)

//
// EXECUTION DEPENDENCIES
//
libraryDependencies += "org.apache.commons" % "commons-exec" % "1.3"

//
// CLI DEPENDENCIES
//
libraryDependencies += "net.sf.jopt-simple" % "jopt-simple" % "4.6" // MIT

//
// MAGIC DEPENDENCIES
//
libraryDependencies ++= Seq(
  // Used to find and download jars from Maven-based repositories
  "org.apache.ivy" % "ivy" % "2.4.0-rc1" // Apache v2,

  // Used for multi class loader implementation
//  "org.apache.openjpa" % "openjpa" % "2.4.0" // Apache v2
)

// Brought in in order to simplify the reading of each project's ivy.xml file
// from the classpath. If we really want we can write our own class and remove
// this dependency but the wheel has already been invented.
libraryDependencies += "org.springframework" % "spring-core" % "4.1.1.RELEASE" // Apache v2
