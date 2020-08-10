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

// Provides the ability to generate unifed documentation for multiple projects
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.3")

// Provides abilit to create an uber-jar
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.15.0")

// Provides a generated build info object to sync between build and application
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")

//  Used for signing jars published via `sbt publish-signed`
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.1")

// Provides the ability to generate dependency graphs
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")
