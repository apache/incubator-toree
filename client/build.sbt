
// Main library dependencies to function
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.6",
  "com.typesafe.akka" %% "akka-zeromq" % "2.3.6",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.6",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.6" % "test",
  "org.slf4j" % "slf4j-api" % "1.7.7"
)

//  Java Test Dependencies
libraryDependencies ++= Seq(
  "org.testng" % "testng" % "6.8.5" % "test",
  "org.easytesting" % "fest-assert" % "1.4" % "test",
  "org.fluentlenium" % "fluentlenium-testng" % "0.9.0" % "test",
  "org.fluentlenium" % "fluentlenium-festassert" % "0.9.0" % "test",
  "org.fluentlenium" % "fluentlenium-core" % "0.9.0" % "test"
)
