import sbt.Tests.{Group, SubProcess}

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
// The chunk below is used to ensure that the test tagged with SbtForked ACTUALLY runs with
// a forked jvm,


def isAnnotatedWithRequiresSpark(definition: xsbti.api.Definition): Boolean = {
  definition.annotations().exists { annotation: xsbti.api.Annotation =>
    annotation.base match {
      case proj: xsbti.api.Projection if (proj.id() == "SbtForked") => true
      case _ => false
    }
  }
}

// Note the type TaskKey[Seq[String]] must be explicitly specified otherwise an error occurs
lazy val testsAnnotatedWithRequiresSpark: TaskKey[Seq[String]] = taskKey[Seq[String]]("Returns list of FQCNs of tests annotated with SbtForked")
testsAnnotatedWithRequiresSpark := {
  val analysis = (compile in Test).value
  analysis.apis.internal.values.flatMap({ source =>
    source.api().definitions().filter(isAnnotatedWithRequiresSpark).map(_.name())
  }).toSeq
}


def forkedJvmPerTest(testDefs: Seq[TestDefinition], testsToFork: Seq[String]) = {

  val (forkedTests, otherTests) = testDefs.partition { testDef => testsToFork.contains(testDef.name) }

  val otherTestsGroup = new Group(name = "Single JVM tests", tests = otherTests, runPolicy = SubProcess(javaOptions = Seq.empty[String]))
  val forkedTestGroups = forkedTests map { test =>
    new Group(
      name = test.name,
      tests = Seq(test),
      runPolicy = SubProcess(javaOptions = Seq.empty[String]))
  }
  Seq(otherTestsGroup) ++ forkedTestGroups
}

testGrouping in Test <<= (definedTests in Test, testsAnnotatedWithRequiresSpark) map forkedJvmPerTest
