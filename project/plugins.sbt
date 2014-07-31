logLevel := Level.Warn

// Provides the ability to create an uber-jar using `sbt assembly`
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")

// Provides the ability to create an IntelliJ project using `sbt gen-idea`
addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

// Provides the ability to list dependencies in a readable format using
// `sbt dependencyTree`; there are other commands provided as well
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

// Provides ability to view code coverage via `sbt jacoco:cover`
addSbtPlugin("de.johoop" % "jacoco4sbt" % "2.1.6") // Eclipse Public License v1.0
