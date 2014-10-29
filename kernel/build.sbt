
//
// JSON DEPENDENCIES
//
libraryDependencies +=
  "com.typesafe.play" %% "play-json" % "2.3.1" // Apache v2

//
// TEST DEPENDENCIES
//
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.0" % "test", // Apache v2
  "org.scalactic" %% "scalactic" % "2.2.0" % "test", // Apache v2
  "org.mockito" % "mockito-all" % "1.9.5" % "test"   // MIT
)

