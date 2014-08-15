package com.ibm.spark.magic.builtin

import java.net.URL

import com.ibm.spark.interpreter.Interpreter
import com.ibm.spark.magic.dependencies.{IncludeInterpreter, IncludeSparkContext}
import org.apache.spark.SparkContext
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import org.mockito.Mockito._
import org.mockito.Matchers._

class AddJarTest extends FunSpec with Matchers with MockitoSugar {

  val PATH_TO_JAR: String = "/path/to/some/jar/somejar.jar"

  describe("AddJar"){
    describe("#executeCell") {
      it("should call addJar on the provided SparkContext and addJars on the provided interpreter") {

        val mockSparkContext = mock[SparkContext]
        val mockInterpreter = mock[Interpreter]

        val addJarMagic = new AddJar with IncludeSparkContext with IncludeInterpreter {
          override val sparkContext: SparkContext = mockSparkContext
          override val interpreter: Interpreter = mockInterpreter
        }

        addJarMagic.executeCell(Seq(
          """http://www.example.com/someJar.jar""",
          """http://www.example.com/someJar.jar""",
          """http://www.example.com/someJar.jar"""
        ))

        verify(mockSparkContext, times(3)).addJar(anyString())
        verify(mockInterpreter, times(3)).addJars(any[URL])
      }
    }

    describe("#executeLine") {
      it("should call addJar on the provided SparkContext and addJars on the provided interpreter") {

        val mockSparkContext = mock[SparkContext]
        val mockInterpreter = mock[Interpreter]

        val addJarMagic = new AddJar with IncludeSparkContext with IncludeInterpreter {
          override val sparkContext: SparkContext = mockSparkContext
          override val interpreter: Interpreter = mockInterpreter
        }

        addJarMagic.executeLine("""http://www.example.com/someJar.jar""")

        verify(mockSparkContext).addJar(anyString())
        verify(mockInterpreter).addJars(any[URL])
      }
    }
  }
}
