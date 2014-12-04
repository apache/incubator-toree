import Common._
import xerial.sbt.Pack._

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