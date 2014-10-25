import de.johoop.testngplugin.TestNGPlugin._
testNGSettings

//  Java Test Dependencies
libraryDependencies ++= Seq(
  "org.testng" % "testng" % "6.8.5" % "test",
  "org.mockito" % "mockito-all" % "1.9.5" % "test",
  "org.easytesting" % "fest-assert" % "1.4" % "test",
  "org.fluentlenium" % "fluentlenium-testng" % "0.9.0" % "test",
  "org.fluentlenium" % "fluentlenium-festassert" % "0.9.0" % "test",
  "org.fluentlenium" % "fluentlenium-core" % "0.9.0" % "test"
)
