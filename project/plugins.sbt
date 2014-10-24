logLevel := Level.Warn

resolvers += Classpaths.sbtPluginReleases

// Provides the ability to create an IntelliJ project using `sbt gen-idea`
addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

// Provides the ability to list dependencies in a readable format using
// `sbt dependencyTree`; there are other commands provided as well
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

// Provides ability to view code coverage via `sbt scoverage:test`
addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "0.99.5.1")

// Provides ability to create a pack containg all jars and a script to run them
// using `sbt pack` or `sbt pack-archive` to generate a *.tar.gz file
addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.6.1")

//  Provides the ability to run java testng tests
addSbtPlugin("de.johoop" % "sbt-testng-plugin" % "3.0.2")

//  Provides the ability to package our project as a docker image
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "0.5.2")