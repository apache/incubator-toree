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
import sbt._
import sbt.Keys._

object CommonPlugin extends AutoPlugin {

  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  object autoImport {
    lazy val UnitTest = config("unit") extend Test
    lazy val IntegrationTest = config("integration") extend Test
    lazy val SystemTest = config("system") extend Test
  }
  import autoImport._

  override def projectSettings = {
    inConfig(UnitTest)(Defaults.testSettings) ++
    inConfig(IntegrationTest)(Defaults.testSettings) ++
    inConfig(SystemTest)(Defaults.testSettings) ++
    Seq(
      testOptions in UnitTest := Seq(Tests.Filter(unitFilter)),
      testOptions in IntegrationTest := Seq(Tests.Filter(intFilter)),
      testOptions in SystemTest := Seq(Tests.Filter(sysFilter)),
      // Add a global resource directory with compile/ and test/ for resources in all projects
      unmanagedResourceDirectories in Compile ++= Seq(
        (baseDirectory in ThisBuild).value / "resources" / "compile"
      ),
      unmanagedResourceDirectories in Test ++= Seq(
        (baseDirectory in ThisBuild).value / "resources" / "test"
      ),
      resourceGenerators in Compile += Def.task {
        val inputFile = (deliverLocal in Compile).value
        val outputFile = (resourceManaged in Compile).value / s"${name.value}-ivy.xml"
        streams.value.log.info(s"Copying ${inputFile.getPath} to ${outputFile.getPath}")
        IO.copyFile(inputFile, outputFile)
        Seq(outputFile)
      }.taskValue
    )
  }

  def sysFilter(name: String): Boolean =
    (name endsWith "SpecForSystem") || (name startsWith "system.")
  def intFilter(name: String): Boolean =
    (name endsWith "SpecForIntegration") || (name startsWith "integration.")
  def unitFilter(name: String): Boolean =
    (name endsWith "Spec") && !intFilter(name) && !sysFilter(name)

}
