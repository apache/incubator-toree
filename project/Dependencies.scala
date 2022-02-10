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

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % "2.5.31" // Apache v2
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % "2.5.31" // Apache v2
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % "2.5.31" // Apache v2

  val clapper = "org.clapper" %% "classutil" % "1.5.1" // BSD 3-clause license, used for detecting plugins

  val commonsExec = "org.apache.commons" % "commons-exec" % "1.3" // Apache v2

  val config = "com.typesafe" % "config" % "1.3.0" // Apache v2

  val coursierVersion = "1.0.3"
  val coursier = "io.get-coursier" %% "coursier" % coursierVersion // Apache v2
  val coursierCache = "io.get-coursier" %% "coursier-cache" % coursierVersion // Apache v2

  val ivy = "org.apache.ivy" % "ivy" % "2.4.0" // Apache v2

  // use the same jackson version in test than the one provided at runtime by Spark 3.0.0
  val jacksonDatabind = "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.0" // Apache v2

  val jeroMq = "org.zeromq" % "jeromq" % "0.4.3" // MPL v2

  val joptSimple = "net.sf.jopt-simple" % "jopt-simple" % "4.9" // MIT

  val mockito = "org.mockito" % "mockito-all" % "1.10.19" // MIT

  val playJson = "com.typesafe.play" %% "play-json" % "2.7.4" // Apache v2

  val scalaCompiler = Def.setting{ "org.scala-lang" % "scala-compiler" % scalaVersion.value } // BSD 3-clause
  val scalaLibrary = Def.setting{ "org.scala-lang" % "scala-library" % scalaVersion.value } // BSD 3-clause
  val scalaReflect = Def.setting{ "org.scala-lang" % "scala-reflect" % scalaVersion.value } // BSD 3-clause

  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8" // Apache v2

  val slf4jApi = "org.slf4j" % "slf4j-api" % "1.7.30" // MIT

  val sparkVersion = settingKey[String]("Version of Apache Spark to use in Toree") // defined in root build
  val sparkCore = Def.setting{ "org.apache.spark" %% "spark-core" % sparkVersion.value } // Apache v2
  val sparkGraphX = Def.setting{ "org.apache.spark" %% "spark-graphx" % sparkVersion.value } // Apache v2
  val sparkMllib = Def.setting{ "org.apache.spark" %% "spark-mllib" % sparkVersion.value } // Apache v2
  val sparkRepl = Def.setting{ "org.apache.spark" %% "spark-repl" % sparkVersion.value } // Apache v2
  val sparkSql = Def.setting{ "org.apache.spark" %% "spark-sql" % sparkVersion.value } // Apache v2
  val sparkStreaming = Def.setting{ "org.apache.spark" %% "spark-streaming" % sparkVersion.value } // Apache v2

  val springCore = "org.springframework" % "spring-core" % "5.2.2.RELEASE"// Apache v2

  // NB: Updated from 14.0.1 as suggested in
  //       https://github.com/google/guava/issues/3249,
  //       https://github.com/google/guava/issues/3345
  //     to prevent test org.apache.toree.magic.builtin.BuiltinLoaderSpec
  //     from failing on OpenJDK 11.
  val guava = "com.google.guava" % "guava" % "31.0.1-jre" // Apache v2

  // Projects

  val sparkAll = Def.setting{
    Seq(
      sparkCore.value % "provided" excludeAll(
        // Exclude netty (org.jboss.netty is for 3.2.2.Final only)
        ExclusionRule(
          organization = "org.jboss.netty",
          name = "netty"
        )
      ),
      sparkGraphX.value % "provided",
      sparkMllib.value % "provided",
      sparkRepl.value % "provided",
      sparkSql.value % "provided",
      sparkStreaming.value % "provided"
    )
  }

}
