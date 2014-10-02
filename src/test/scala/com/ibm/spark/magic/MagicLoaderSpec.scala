package com.ibm.spark.magic

import java.io.OutputStream
import java.net.URL

import com.ibm.spark.interpreter.Interpreter
import com.ibm.spark.magic.builtin.{BuiltinLoader, MagicTemplate}
import com.ibm.spark.magic.dependencies.DependencyMap
import org.apache.spark.SparkContext
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

class MagicWithNoDependencies extends MagicTemplate {
  /**
   * Execute a magic representing a line magic.
   * @param code The single line of code
   * @return The output of the magic
   */
  override def executeLine(code: String): MagicOutput = MagicOutput()

  /**
   * Execute a magic representing a cell magic.
   * @param code The list of code, separated by newlines
   * @return The output of the magic
   */
  override def executeCell(code: Seq[String]): MagicOutput = MagicOutput()
}

class MagicLoaderSpec extends FunSpec with Matchers with MockitoSugar {
  describe("MagicLoader") {
    describe("#hasMagic") {
      it("should return false if a class with the magic name is not found") {
        val magicLoader = new MagicLoader() {
          override def findClass(name: String): Class[_] = throw new ClassNotFoundException()
        }

        magicLoader.hasMagic("potato") should be (false)
      }

      it("should return true if a class with the magic name is found") {
        val magicLoader = new MagicLoader() {
          override def findClass(name: String): Class[_] = this.getClass
        }

        magicLoader.hasMagic("potato") should be (true)
      }
    }

    describe("#executeMagic") {
      it("should execute the line magic if isCell == false") {
        val name = "potato"
        val code = "cheese"
        val isCell = false

        val mockMagic = mock[MagicTemplate]

        val magicLoader = new MagicLoader() {
          override protected def createMagicInstance(name: String): Any = mockMagic
        }

        magicLoader.executeMagic(name, code, isCell)
        verify(mockMagic).executeLine(code)
      }

      it("should execute the cell magic if isCell == true") {
        val name = "potato"
        val code = Seq("cheese", "is", "delicious")
        val isCell = true

        val mockMagic = mock[MagicTemplate]

        val magicLoader = new MagicLoader() {
          override protected def createMagicInstance(name: String): Any = mockMagic
        }

        magicLoader.executeMagic(name, code.mkString("\n"), isCell)
        verify(mockMagic).executeCell(code)
      }

      it("should correctly load a class to execute") {
        val mockInterpreter = mock[Interpreter]
        val mockSparkContext = mock[SparkContext]
        val mockOutputStream = mock[OutputStream]

        val dependencyMap = new DependencyMap()
          .setInterpreter(mockInterpreter)
          .setSparkContext(mockSparkContext)
          .setOutputStream(mockOutputStream)

        val magicLoader = new MagicLoader(
          dependencyMap = dependencyMap,
          parentLoader = new BuiltinLoader
        )

        magicLoader.executeMagic("AddJar", "http://www.example.com", false)
        verify(mockInterpreter).addJars(any[URL])
        verify(mockSparkContext).addJar(anyString())
      }
    }
  }

}
