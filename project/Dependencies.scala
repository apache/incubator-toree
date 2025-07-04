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
import sbt._
import sbt.Keys._
import scala.util.Properties

object Dependencies {

  // Libraries
  val pekkoVersion = "1.0.3" // Apache v2
  val pekkoActor = "org.apache.pekko" %% "pekko-actor" % pekkoVersion // Apache v2
  val pekkoSlf4j = "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion // Apache v2
  val pekkoTestkit = "org.apache.pekko" %% "pekko-testkit" % pekkoVersion // Apache v2

  val clapper = "org.clapper" %% "classutil" % "1.5.1" // BSD 3-clause license, used for detecting plugins

  val commonsExec = "org.apache.commons" % "commons-exec" % "1.3" // Apache v2

  val config = "com.typesafe" % "config" % "1.4.3" // Apache v2

  val coursierVersion = "2.0.0"
  val coursier = "io.get-coursier" %% "coursier" % coursierVersion // Apache v2
  val coursierCache = "io.get-coursier" %% "coursier-cache" % coursierVersion // Apache v2

  val ivy = "org.apache.ivy" % "ivy" % "2.5.1" // Apache v2

  // use the same jackson version in test than the one provided at runtime by Spark 3.3.2
  val jacksonDatabind = "com.fasterxml.jackson.core" % "jackson-databind" % "2.13.4.2" // Apache v2

  val jeroMq = "org.zeromq" % "jeromq" % "0.5.3" // MPL v2

  val joptSimple = "net.sf.jopt-simple" % "jopt-simple" % "4.9" // MIT

  val playJson = "com.typesafe.play" %% "play-json" % "2.7.4" // Apache v2

  val scalaCompiler = Def.setting{ "org.scala-lang" % "scala-compiler" % scalaVersion.value } // BSD 3-clause
  val scalaLibrary = Def.setting{ "org.scala-lang" % "scala-library" % scalaVersion.value } // BSD 3-clause
  val scalaReflect = Def.setting{ "org.scala-lang" % "scala-reflect" % scalaVersion.value } // BSD 3-clause

  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.16" // Apache v2
  val scalaTestMockito = "org.scalatestplus" %% "mockito-4-11" % "3.2.16.0" // Apache v2
  val mockitoInline = "org.mockito" % "mockito-inline" % "4.11.0" // MIT

  val slf4jApi = "org.slf4j" % "slf4j-api" % "1.7.32" // MIT

  val sparkVersion = settingKey[String]("Version of Apache Spark to use in Toree") // defined in root build
  val sparkCore = Def.setting{ "org.apache.spark" %% "spark-core" % sparkVersion.value } // Apache v2
  val sparkGraphX = Def.setting{ "org.apache.spark" %% "spark-graphx" % sparkVersion.value } // Apache v2
  val sparkMllib = Def.setting{ "org.apache.spark" %% "spark-mllib" % sparkVersion.value } // Apache v2
  val sparkRepl = Def.setting{ "org.apache.spark" %% "spark-repl" % sparkVersion.value } // Apache v2
  val sparkSql = Def.setting{ "org.apache.spark" %% "spark-sql" % sparkVersion.value } // Apache v2
  val sparkStreaming = Def.setting{ "org.apache.spark" %% "spark-streaming" % sparkVersion.value } // Apache v2

  val springCore = "org.springframework" % "spring-core" % "5.2.2.RELEASE"// Apache v2

  // TOREE-552 Align Guava with Spark
  val guava = "com.google.guava" % "guava" % "14.0.1" // Apache v2

  // Projects
  val sparkAll = Def.setting{
    Seq(
      sparkCore.value % "provided",
      sparkGraphX.value % "provided",
      sparkMllib.value % "provided",
      sparkRepl.value % "provided",
      sparkSql.value % "provided",
      sparkStreaming.value % "provided"
    )
  }

}
