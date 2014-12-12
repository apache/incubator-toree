import Common._
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
fork := true // http://www.scala-sbt.org/0.13/docs/Running-Project-Code.html#Deserialization+and+class+loading

pack <<= pack dependsOn (rebuildIvyXml dependsOn deliverLocal)

packArchive <<= packArchive dependsOn (rebuildIvyXml dependsOn deliverLocal)

//
// AKKA DEPENDENCIES (from Spark project)
//
libraryDependencies +=
  "org.spark-project.akka" %% "akka-zeromq" % "2.2.3-shaded-protobuf" // Apache v2

//
// TEST DEPENDENCIES
//
libraryDependencies +=
  "org.spark-project.akka" %% "akka-testkit" % "2.2.3-shaded-protobuf" % "test" // MIT

//
// CUSTOM TASKS
//

lazy val kill = taskKey[Unit]("Executing the shell script.")

kill := {
  "sh scripts/terminate_spark_kernels.sh".!
}