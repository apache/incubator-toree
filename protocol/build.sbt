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

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

//
// JSON DEPENDENCIES
//
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.3.6" excludeAll( // Apache v2
      ExclusionRule(organization = "com.fasterxml.jackson.core")
    ),
  "org.slf4j" % "slf4j-api" % "1.7.5" // MIT
)

//
// TEST DEPENDENCIES
//
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.0" % "test", // Apache v2
  "org.scalactic" %% "scalactic" % "2.2.0" % "test", // Apache v2
  "org.mockito" % "mockito-all" % "1.9.5" % "test"   // MIT
)
