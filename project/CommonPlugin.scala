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
import sbt.{Def, _}
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

  override def projectSettings: Seq[Def.Setting[_]] = {
    inConfig(UnitTest)(Defaults.testSettings) ++
    inConfig(IntegrationTest)(Defaults.testSettings) ++
    inConfig(SystemTest)(Defaults.testSettings) ++
    Seq(
      UnitTest / testOptions := Seq(Tests.Filter(unitFilter)),
      IntegrationTest / testOptions := Seq(Tests.Filter(intFilter)),
      SystemTest / testOptions := Seq(Tests.Filter(sysFilter)),
      // Add a global resource directory with compile/ and test/ for resources in all projects
      Compile / unmanagedResourceDirectories ++= Seq(
        (ThisBuild / baseDirectory).value / "resources" / "compile"
      ),
      Test / unmanagedResourceDirectories ++= Seq(
        (ThisBuild / baseDirectory).value / "resources" / "test"
      ),
      Compile / resourceGenerators += Def.task {
        val inputFile = (Compile / deliverLocal).value
        val outputFile = (Compile / resourceManaged).value / s"${name.value}-ivy.xml"
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
