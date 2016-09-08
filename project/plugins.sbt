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

logLevel := Level.Warn

resolvers += Classpaths.sbtPluginReleases

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

resolvers += "Apache Snapshots" at "http://repository.apache.org/snapshots/"

// Provides the ability to create an IntelliJ project using `sbt gen-idea`
addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

// Provides the ability to generate unifed documentation for multiple projects
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.1")

// Provides the ability to list dependencies in a readable format using
// `sbt dependencyTree`; there are other commands provided as well
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

// Provides abilit to create an uber-jar
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.0")

// Provides a generated build info object to sync between build and application
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")

// Provides code coverage support
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.0.4")

// Provides coveralls integration (for use with Travis-ci)
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.0.0.BETA1")

// Provides site generation functionality
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.8.1")

// Provides auto-generating and publishing a gh-pages site
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.3")

// Provides alternative resolving/downloading over sbt
//addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M12")

//  Used for signing jars published via `sbt publish-signed`
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
