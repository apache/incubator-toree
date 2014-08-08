package integration.interpreter

import java.io.OutputStream
import scala.tools.nsc.interpreter._
import java.net.URLClassLoader

import com.ibm.spark.interpreter.ScalaInterpreter
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpec}

class AddExternalJarMagicSpec extends FunSpec with Matchers with MockitoSugar {
  describe("ScalaInterpreter") {
    describe("#addJars") {
      it("should be able to load an external jar") {
        val testJarUrl = this.getClass.getClassLoader.getResource("TestJar.jar")
        val interpreter = new ScalaInterpreter(List(), Console.out)
        //val interpreter = new ScalaInterpreter(List(), mock[OutputStream])
        interpreter.start()

        interpreter.interpret("import com.ibm.testjar.TestClass")._1 should be (IR.Error)
        interpreter.addJars(testJarUrl)
        interpreter.interpret("""
            println(this.getClass.getClassLoader.getResource("TestJar.jar"))
        """)
        interpreter.interpret("import com.ibm.testjar.TestClass")._1 should be (IR.Success)
      }
    }
  }
}
