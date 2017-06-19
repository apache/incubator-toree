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

import java.io.{ByteArrayOutputStream, OutputStream}

import org.apache.toree.annotations.SbtForked
import org.apache.toree.global.StreamState
import org.apache.toree.interpreter._
import org.apache.toree.kernel.api.KernelLike
import org.apache.toree.kernel.interpreter.scala.ScalaInterpreter
import org.apache.toree.utils.{MultiOutputStream, TaskManager}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

@SbtForked
class AddExternalJarMagicSpecForIntegration
  extends FunSpec with Matchers with MockitoSugar with BeforeAndAfter
{

  private val outputResult = new ByteArrayOutputStream()
  private var interpreter: Interpreter = _

  before {
    interpreter = new ScalaInterpreter {
      override protected def bindKernelVariable(kernel: KernelLike): Unit = { }
    }
    // interpreter.start()
    interpreter.init(mock[KernelLike])

    StreamState.setStreams(outputStream = outputResult)
  }

  after {
    interpreter.stop()
    outputResult.reset()
  }

  describe("ScalaInterpreter") {
    describe("#addJars") {
      it("should be able to load an external jar") {
        val testJarUrl = this.getClass.getClassLoader.getResource("TestJar.jar")

        //
        // NOTE: This can be done with any jar. I have tested it previously by
        //       downloading jgoodies, placing it in /tmp/... and loading it.
        //

        // Should fail since jar was not added to paths
        interpreter.interpret(
          "import com.ibm.testjar.TestClass")._1 should be (Results.Error)

        // Add jar to paths
        interpreter.addJars(testJarUrl)

        // Should now succeed
        interpreter.interpret(
          "import com.ibm.testjar.TestClass")._1 should be (Results.Success)

        // Should now run
        interpreter.interpret(
          """println(new TestClass().sayHello("Chip"))"""
        ) should be ((Results.Success, Left(Map())))
        outputResult.toString should be ("Hello, Chip\n")
      }

      it("should support Scala jars") {
        val testJarUrl = this.getClass.getClassLoader.getResource("ScalaTestJar.jar")

        // Should fail since jar was not added to paths
        interpreter.interpret(
          "import com.ibm.scalatestjar.TestClass")._1 should be (Results.Error)

        // Add jar to paths
        interpreter.addJars(testJarUrl)

        // Should now succeed
        interpreter.interpret(
          "import com.ibm.scalatestjar.TestClass")._1 should be (Results.Success)

        // Should now run
        interpreter.interpret(
          """println(new TestClass().runMe())"""
        ) should be ((Results.Success, Left(Map())))
        outputResult.toString should be ("You ran me!\n")
      }

      it("should be able to add multiple jars at once") {
        val testJar1Url =
          this.getClass.getClassLoader.getResource("TestJar.jar")
        val testJar2Url =
          this.getClass.getClassLoader.getResource("TestJar2.jar")
//        val interpreter = new ScalaInterpreter(List(), mock[OutputStream])
//          with StandardSparkIMainProducer
//          with StandardTaskManagerProducer
//          with StandardSettingsProducer
//        interpreter.start()

        // Should fail since jars were not added to paths
        interpreter.interpret(
          "import com.ibm.testjar.TestClass")._1 should be (Results.Error)
        interpreter.interpret(
          "import com.ibm.testjar2.TestClass")._1 should be (Results.Error)

        // Add jars to paths
        interpreter.addJars(testJar1Url, testJar2Url)

        // Should now succeed
        interpreter.interpret(
          "import com.ibm.testjar.TestClass")._1 should be (Results.Success)
        interpreter.interpret(
          "import com.ibm.testjar2.TestClass")._1 should be (Results.Success)

        // Should now run
        interpreter.interpret(
          """println(new com.ibm.testjar.TestClass().sayHello("Chip"))"""
        ) should be ((Results.Success, Left(Map())))
        outputResult.toString should be ("Hello, Chip\n")
        outputResult.reset()

        interpreter.interpret(
          """println(new com.ibm.testjar2.TestClass().CallMe())"""
        ) should be ((Results.Success, Left(Map())))
        outputResult.toString should be ("3\n")
      }

      it("should be able to add multiple jars in consecutive calls to addjar") {
        val testJar1Url =
          this.getClass.getClassLoader.getResource("TestJar.jar")
        val testJar2Url =
          this.getClass.getClassLoader.getResource("TestJar2.jar")
//        val interpreter = new ScalaInterpreter(List(), mock[OutputStream])
//          with StandardSparkIMainProducer
//          with StandardTaskManagerProducer
//          with StandardSettingsProducer
//        interpreter.start()

        // Should fail since jars were not added to paths
        interpreter.interpret(
          "import com.ibm.testjar.TestClass")._1 should be (Results.Error)
        interpreter.interpret(
          "import com.ibm.testjar2.TestClass")._1 should be (Results.Error)

        // Add jars to paths
        interpreter.addJars(testJar1Url)
        interpreter.addJars(testJar2Url)

        // Should now succeed
        interpreter.interpret(
          "import com.ibm.testjar.TestClass")._1 should be (Results.Success)
        interpreter.interpret(
          "import com.ibm.testjar2.TestClass")._1 should be (Results.Success)

        // Should now run
        interpreter.interpret(
          """println(new com.ibm.testjar.TestClass().sayHello("Chip"))"""
        ) should be ((Results.Success, Left(Map())))
        outputResult.toString should be ("Hello, Chip\n")
        outputResult.reset()

        interpreter.interpret(
          """println(new com.ibm.testjar2.TestClass().CallMe())"""
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
