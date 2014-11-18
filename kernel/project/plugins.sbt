logLevel := Level.Warn

resolvers += Classpaths.sbtPluginReleases

// Provides ability to create a pack containing all jars and a script to run
// them using `sbt pack` or `sbt pack-archive` to generate a *.tar.gz file
addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.6.1")
