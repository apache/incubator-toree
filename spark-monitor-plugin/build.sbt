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

Test / fork := true

// Needed for SparkMonitor plugin
libraryDependencies ++= Dependencies.sparkAll.value
libraryDependencies ++= Seq(
  "org.json4s" % "json4s-native_2.12" % "3.7.0-M11" % "provided",
  ("org.json4s" % "json4s-jackson_2.12" % "3.7.0-M11" % "provided").exclude("com.fasterxml.jackson.core" , "jackson-databind"),
)

// Test dependencies
libraryDependencies += Dependencies.scalaCompiler.value % "test"

// Assembly configuration for separate jar
enablePlugins(AssemblyPlugin)

assembly / assemblyMergeStrategy := {
  case "module-info.class" => MergeStrategy.discard
  case PathList("META-INF", "versions", "9", "module-info.class") => MergeStrategy.discard
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

assembly / assemblyOption ~= {
  _.withIncludeScala(false)
}

assembly / test := {}