package integration.interpreter

import java.io.{OutputStream, ByteArrayOutputStream}
import java.util.UUID

import com.ibm.spark.interpreter._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

class AddExternalJarMagicSpecForIntegration
  extends FunSpec with Matchers with MockitoSugar with BeforeAndAfter
{

  private val outputResult = new ByteArrayOutputStream()
  private var interpreter: Interpreter = _

  before {
    interpreter = new ScalaInterpreter(Nil, mock[OutputStream])
      with StandardSparkIMainProducer
      with StandardTaskManagerProducer
      with StandardSettingsProducer
    interpreter.start()

    // TODO: Provide better system to wrap this output or consider using
    //       contains(...) with results to match 'res3: String = "..."'
    val randomVarName = "$var" + UUID.randomUUID().toString.replace("-", "")
    interpreter.bind(
      s"$randomVarName", outputResult.getClass.getName, outputResult, Nil)
    interpreter.interpret(s"Console.setOut($randomVarName)")
  }

  after {
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
        ) should be ((Results.Success, Left("")))
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
        ) should be ((Results.Success, Left("")))
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
        ) should be ((Results.Success, Left("")))
        outputResult.toString should be ("Hello, Chip\n")
        outputResult.reset()

        interpreter.interpret(
          """println(new com.ibm.testjar2.TestClass().CallMe())"""
        ) should be ((Results.Success, Left("")))
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
        ) should be ((Results.Success, Left("")))
        outputResult.toString should be ("Hello, Chip\n")
        outputResult.reset()

        interpreter.interpret(
          """println(new com.ibm.testjar2.TestClass().CallMe())"""
        ) should be ((Results.Success, Left("")))
        outputResult.toString should be ("3\n")
      }
    }
  }
}
