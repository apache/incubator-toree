/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

libraryDependencies ++= Dependencies.sparkAll.value

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
  Dependencies.scalaLibrary.value,
  Dependencies.scalaCompiler.value % "scala-tool",
  Dependencies.scalaReflect.value
)

//
// EXECUTION DEPENDENCIES
//
libraryDependencies += Dependencies.commonsExec

//
// CLI DEPENDENCIES
//
libraryDependencies += Dependencies.joptSimple


libraryDependencies += Dependencies.config

//
// MAGIC DEPENDENCIES
//
libraryDependencies ++= Seq(
  Dependencies.coursier,
  Dependencies.coursierCache
)

// Brought in in order to simplify the reading of each project's ivy.xml file
// from the classpath. If we really want we can write our own class and remove
// this dependency but the wheel has already been invented.
libraryDependencies += Dependencies.springCore // Apache v2
