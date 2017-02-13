import sbt._
import sbt.Keys._
import scala.util.Properties

object Dependencies {

  val sparkVersion = Def.setting{
    //sLog.value.warn("danger!")
    val envVar = "APACHE_SPARK_VERSION"
    val defaultVersion = "2.0.0"

    Properties.envOrNone(envVar) match {
      case None =>
        sLog.value.info(s"Using default Apache Spark version $defaultVersion!")
        defaultVersion
      case Some(version) =>
        sLog.value.info(s"Using Apache Spark version $version, provided from $envVar")
        version
    }
  }

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % "2.4.17"
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % "2.4.17"
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % "2.4.17"

  val clapper = "org.clapper" %% "classutil" % "1.0.12" // BSD 3-clause license, used for detecting plugins

  val commonsExec = "org.apache.commons" % "commons-exec" % "1.3"

  val config = "com.typesafe" % "config" % "1.3.0"

  // Apache v2
  val coursier = "io.get-coursier" %% "coursier" % "1.0.0-M15-1"
  val coursierCache = "io.get-coursier" %% "coursier-cache" % "1.0.0-M15-1"

  val ivy = "org.apache.ivy" % "ivy" % "2.4.0-rc1" // Apache v2

  // use the same jackson version in test than the one provided at runtime by Spark 2.0.0
  val jacksonDatabind = "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.5" // Apache v2

  val jeroMq = "org.zeromq" % "jeromq" % "0.3.6"

  val joptSimple = "net.sf.jopt-simple" % "jopt-simple" % "4.6" // MIT

  val mockito = "org.mockito" % "mockito-all" % "1.10.19" // MIT

  val playJson = "com.typesafe.play" %% "play-json" % "2.3.10" // Apache v2

  val scalaCompiler = Def.setting{ "org.scala-lang" % "scala-compiler" % scalaVersion.value }
  val scalaLibrary = Def.setting{ "org.scala-lang" % "scala-library" % scalaVersion.value }
  val scalaReflect = Def.setting{ "org.scala-lang" % "scala-reflect" % scalaVersion.value }

  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.6" // Apache v2

  val slf4jApi = "org.slf4j" % "slf4j-api" % "1.7.21" // MIT

  val sparkCore = Def.setting{ "org.apache.spark" %% "spark-core" % sparkVersion.value }
  val sparkGraphX = Def.setting{ "org.apache.spark" %% "spark-graphx" % sparkVersion.value }
  val sparkMllib = Def.setting{ "org.apache.spark" %% "spark-mllib" % sparkVersion.value }
  val sparkRepl = Def.setting{ "org.apache.spark" %% "spark-repl" % sparkVersion.value }
  val sparkSql = Def.setting{ "org.apache.spark" %% "spark-sql" % sparkVersion.value }
  val sparkStreaming = Def.setting{ "org.apache.spark" %% "spark-streaming" % sparkVersion.value }

  val springCore = "org.springframework" % "spring-core" % "4.1.1.RELEASE"// Apache v2

}
