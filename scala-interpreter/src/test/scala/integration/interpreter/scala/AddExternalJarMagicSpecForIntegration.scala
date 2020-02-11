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

package integration.interpreter.scala

import java.io.{ByteArrayOutputStream, File}

import org.apache.spark.toree.test.utils.JarUtils
import org.apache.toree.annotations.SbtForked
import org.apache.toree.global.StreamState
import org.apache.toree.interpreter._
import org.apache.toree.kernel.api.KernelLike
import org.apache.toree.kernel.interpreter.scala.ScalaInterpreter
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Ignore, Matchers}

@SbtForked
@Ignore
class AddExternalJarMagicSpecForIntegration
  extends FunSpec with Matchers with MockitoSugar with BeforeAndAfter
{

  private val outputResult = new ByteArrayOutputStream()
  private var interpreter: Interpreter = _
  private var tempdir: File = _


  before {
    interpreter = new ScalaInterpreter {
      override protected def bindKernelVariable(kernel: KernelLike): Unit = { }
    }
    // interpreter.start()
    interpreter.init(mock[KernelLike])

    StreamState.setStreams(outputStream = outputResult)

    tempdir = JarUtils.createTemporaryDir()
  }

  after {
    interpreter.stop()
    outputResult.reset()
  }

  describe("ScalaInterpreter") {
    describe("#addJars") {
      it("should do something") {
        interpreter.interpret("1+1")
      }

      it("should be able to load Java jars") {
        val testJar1Url =
          JarUtils.createDummyJar(tempdir.toString, "test1", "TestClass")

        // Should fail since jars were not added to paths
        interpreter.interpret(
          "import test1.TestClass")._1 should be (Results.Error)

        // Add jars to paths
        interpreter.addJars(testJar1Url)

        // Should now succeed
        interpreter.interpret(
          "import test1.TestClass")._1 should be (Results.Success)

        // Should now run
        interpreter.interpret(
          """println(test1.TestClass.sayHello("Chip"))"""
        ) should be ((Results.Success, Left(Map())))
        outputResult.toString should be ("Hello, Chip\n")
        outputResult.reset()

        interpreter.interpret(
          """println(test1.TestClass.addStuff(2,1))"""
        ) should be ((Results.Success, Left(Map())))
        outputResult.toString should be ("3\n")
        outputResult.reset()
      }

      ignore("should support Scala jars") {
        val locationURL = "https://repo1.maven.org/maven2/org/scala-rules/rule-engine-core_2.11/0.5.1/rule-engine-core_2.11-0.5.1.jar"
        val testJarUrl = JarUtils.downloadJar(tempdir.toString, locationURL)

        // Should fail since jar was not added to paths
        interpreter.interpret(
          "import org.scalarules.utils._")._1 should be (Results.Error)

        // Add jar to paths
        interpreter.addJars(testJarUrl)

        // Should now succeed
        interpreter.interpret(
          "import org.scalarules.utils._")._1 should be (Results.Success)

        // Should now run
        /*
        interpreter.interpret(
          """println(new TestClass().runMe())"""
        ) should be ((Results.Success, Left(Map())))
        outputResult.toString should be ("You ran me!\n")
        */
      }

      it("should be able to add multiple jars at once") {
        val testJar1Url =
          JarUtils.createDummyJar(tempdir.toString, "test1", "TestClass")
        val testJar2Url =
          JarUtils.createDummyJar(tempdir.toString, "test2", "TestClass")

        // Should fail since jars were not added to paths
        interpreter.interpret(
          "import test1.TestClass")._1 should be (Results.Error)
        interpreter.interpret(
          "import test2.TestClass")._1 should be (Results.Error)

        // Add jars to paths
        interpreter.addJars(testJar1Url, testJar2Url)

        // Should now succeed
        interpreter.interpret(
          "import test1.TestClass")._1 should be (Results.Success)
        interpreter.interpret(
          "import test2.TestClass")._1 should be (Results.Success)

        // Should now run
        interpreter.interpret(
          """println(test1.TestClass.sayHello("Chip"))"""
        ) should be ((Results.Success, Left(Map())))
        outputResult.toString should be ("Hello, Chip\n")
        outputResult.reset()

        interpreter.interpret(
          """println(test2.TestClass.addStuff(2,1))"""
        ) should be ((Results.Success, Left(Map())))
        outputResult.toString should be ("3\n")
        outputResult.reset()
      }

      it("should be able to add multiple jars in consecutive calls to addjar") {
        val testJar1Url =
          JarUtils.createDummyJar(tempdir.toString, "test1", "TestClass")
        val testJar2Url =
          JarUtils.createDummyJar(tempdir.toString, "test2", "TestClass")

        // Should fail since jars were not added to paths
        interpreter.interpret(
          "import test1.TestClass")._1 should be (Results.Error)
        interpreter.interpret(
          "import test2.TestClass")._1 should be (Results.Error)

        // Add jars to paths
        interpreter.addJars(testJar1Url)
        interpreter.addJars(testJar2Url)

        // Should now succeed
        interpreter.interpret(
          "import test1.TestClass")._1 should be (Results.Success)
        interpreter.interpret(
          "import test2.TestClass")._1 should be (Results.Success)

        // Should now run
        interpreter.interpret(
          """println(test1.TestClass.sayHello("Chip"))"""
        ) should be ((Results.Success, Left(Map())))
        outputResult.toString should be ("Hello, Chip\n")
        outputResult.reset()

        interpreter.interpret(
          """println(test2.TestClass.addStuff(2,1))"""
        ) should be ((Results.Success, Left(Map())))
        outputResult.toString should be ("3\n")
      }

      // Todo: rebinding is kinda finicky in Scala 2.11
      ignore("should not have issues with previous variables") {
        val testJar1Url =
          this.getClass.getClassLoader.getResource("TestJar.jar")
        val testJar2Url =
          this.getClass.getClassLoader.getResource("TestJar2.jar")

        // Add a jar, which reinitializes the symbols
        interpreter.addJars(testJar1Url)

        interpreter.interpret(
          """
            |val t = new com.ibm.testjar.TestClass()
          """.stripMargin)._1 should be (Results.Success)

        // Add a second jar, which reinitializes the symbols and breaks the
        // above variable
        interpreter.addJars(testJar2Url)

        interpreter.interpret(
          """
            |def runMe(testClass: com.ibm.testjar.TestClass) =
            |testClass.sayHello("Hello")
          """.stripMargin)._1 should be (Results.Success)

        // This line should NOT explode if variable is rebound correctly
        // otherwise you get the error of
        //
        // Message: <console>:16: error: type mismatch;
        // found   : com.ibm.testjar.com.ibm.testjar.com.ibm.testjar.com.ibm.
        //           testjar.com.ibm.testjar.TestClass
        // required: com.ibm.testjar.com.ibm.testjar.com.ibm.testjar.com.ibm.
        //           testjar.com.ibm.testjar.TestClass
        // runMe(t)
        //       ^
        val ans = interpreter.interpret(
          """
            |runMe(t)
          """.stripMargin)

        ans._1 should be (Results.Success)
      }
    }
  }
}
