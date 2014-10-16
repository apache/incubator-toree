package com.ibm.spark.magic.builtin

import java.io.OutputStream
import java.net.URL

import com.ibm.spark.interpreter.Interpreter
import com.ibm.spark.magic.dependencies.{IncludeOutputStream, IncludeInterpreter, IncludeSparkContext}
import org.apache.spark.SparkContext
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import org.mockito.Mockito._
import org.mockito.Matchers._

class AddJarSpec extends FunSpec with Matchers with MockitoSugar {
  describe("AddJar"){
    describe("#executeCell") {
      it("should call addJar on the provided SparkContext and addJars on the provided interpreter") {
        val mockSparkContext = mock[SparkContext]
        val mockInterpreter = mock[Interpreter]
        val mockOutputStream = mock[OutputStream]

        val addJarMagic = new AddJar
          with IncludeSparkContext
          with IncludeInterpreter
          with IncludeOutputStream
        {
          override val sparkContext: SparkContext = mockSparkContext
          override val interpreter: Interpreter = mockInterpreter
          override val outputStream: OutputStream = mockOutputStream
          override def downloadFile(fileUrl: URL, destinationUrl: URL): URL =
            new URL("file://someFile") // Cannot mock URL
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
        val mockOutputStream = mock[OutputStream]

        val addJarMagic = new AddJar
          with IncludeSparkContext
          with IncludeInterpreter
          with IncludeOutputStream
        {
          override val sparkContext: SparkContext = mockSparkContext
          override val interpreter: Interpreter = mockInterpreter
          override val outputStream: OutputStream = mockOutputStream
          override def downloadFile(fileUrl: URL, destinationUrl: URL): URL =
            new URL("file://someFile") // Cannot mock URL
        }

        addJarMagic.executeLine("""http://www.example.com/someJar.jar""")

        verify(mockSparkContext).addJar(anyString())
        verify(mockInterpreter).addJars(any[URL])
      }
    }
  }
}
