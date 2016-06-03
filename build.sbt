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

import Common._
import sbtunidoc.Plugin.UnidocKeys._
import sbtunidoc.Plugin._
import com.typesafe.sbt.SbtGit.{GitKeys => git}
import com.typesafe.sbt.SbtSite.site

lazy val root = ToreeProject("toree", ".", doFork=false, needsSpark=true).
  settings(unidocSettings:_*).
  settings(site.settings:_*).
  settings(
    test in assembly := {},
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
    scalacOptions in (ScalaUnidoc, unidoc) += "-Ymacro-no-expand",
    git.gitRemoteRepo := "git://git.apache.org/incubator-toree.git",
    aggregate in assembly := false,
    unmanagedResourceDirectories in Compile += { baseDirectory.value / "dist/toree-legal" }
  ).aggregate(
    macros,protocol,plugins,communication,kernelApi,client,scalaInterpreter,sqlInterpreter,pysparkInterpreter,sparkrInterpreter,kernel
  ).dependsOn(
    macros,protocol,communication,kernelApi,client,scalaInterpreter,sqlInterpreter,pysparkInterpreter,sparkrInterpreter,kernel
  )

/**
  * Project representing macros in Scala that must be compiled separately from
  * any other project using them.
  */
lazy val macros = ToreeProject("macros")

/**
  * Project representing the IPython kernel message protocol in Scala. Used
  * by the client and kernel implementations.
  */
lazy val protocol = ToreeProject("protocol").dependsOn(macros).
  enablePlugins(BuildInfoPlugin).settings(buildInfoSettings:_*)

/**
  * Project representing base plugin system for the Toree infrastructure.
  */
lazy val plugins = ToreeProject("plugins", doFork=true).dependsOn(macros)

/**
  * Project representing forms of communication used as input/output for the
  * client/kernel.
  */
lazy val communication = ToreeProject("communication").dependsOn(macros, protocol)

/**
* Project representing the kernel-api code used by the Spark Kernel. Others can
* import this to implement their own magics and plugins.
*/
lazy val kernelApi = ToreeProject("kernel-api", needsSpark=true).dependsOn(macros, plugins)

/**
* Project representing the client code for connecting to the kernel backend.
*/
lazy val client = ToreeProject("client").dependsOn(macros, protocol, communication)

/**
* Project represents the scala interpreter used by the Spark Kernel.
*/
lazy val scalaInterpreter = ToreeProject("scala-interpreter", needsSpark=true).dependsOn(plugins, protocol, kernelApi)

/**
* Project represents the SQL interpreter used by the Spark Kernel.
*/
lazy val sqlInterpreter = ToreeProject("sql-interpreter", needsSpark=true).dependsOn(plugins, protocol, kernelApi)

/**
* Project represents the Python interpreter used by the Spark Kernel.
*/
lazy val pysparkInterpreter = ToreeProject("pyspark-interpreter", needsSpark=true).dependsOn(plugins, protocol, kernelApi)

/**
* Project represents the R interpreter used by the Spark Kernel.
*/
lazy val sparkrInterpreter = ToreeProject("sparkr-interpreter", needsSpark=true).dependsOn(plugins, protocol, kernelApi)

/**
* Project representing the kernel code for the Spark Kernel backend.
*/
lazy val kernel = ToreeProject("kernel", doFork=true, needsSpark=true).dependsOn(
  macros % "test->test;compile->compile",
  protocol % "test->test;compile->compile",
  communication % "test->test;compile->compile",
  kernelApi % "test->test;compile->compile",
  pysparkInterpreter % "test->test;compile->compile",
  scalaInterpreter % "test->test;compile->compile",
  sparkrInterpreter % "test->test;compile->compile",
  sqlInterpreter % "test->test;compile->compile"
)
