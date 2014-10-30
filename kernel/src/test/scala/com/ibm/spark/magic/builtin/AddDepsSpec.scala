package com.ibm.spark.magic.builtin

import java.io.{ByteArrayOutputStream, OutputStream}
import java.net.URL

import com.ibm.spark.dependencies.DependencyDownloader
import com.ibm.spark.interpreter.Interpreter
import com.ibm.spark.utils.ArgumentParsingSupport
import org.apache.spark.SparkContext
import org.scalatest.mock.MockitoSugar
import org.scalatest.{GivenWhenThen, Matchers, FunSpec}
import org.mockito.Mockito._
import org.mockito.Matchers._

import com.ibm.spark.magic._
import com.ibm.spark.magic.dependencies._

class AddDepsSpec extends FunSpec with Matchers with MockitoSugar
  with GivenWhenThen
{
  describe("AddDeps"){
    describe("#executeCell") {
      it("should invoke executeLine per line of code") {
        Given("Add Dependency Magic")
        val addDepsMagic = spy(new AddDeps)
        doReturn(Map()).when(addDepsMagic).executeLine(anyString())

        When("executeCell is invoked with strings")
        val expected = "one" :: "two" :: "three" :: Nil
        addDepsMagic.executeCell(expected)

        Then("executeLine should be called once per string")
        expected.foreach(s => verify(addDepsMagic).executeLine(s))
      }

      it("should return an empty MagicOutput on success") {
        val addDepsMagic = spy(new AddDeps)
        val expected = MagicOutput()

        addDepsMagic.executeCell(Nil) should be (expected)
      }
    }

    describe("#executeLine") {
      it("should print out the help message if the input is invalid") {
        val byteArrayOutputStream = new ByteArrayOutputStream()

        var printHelpWasRun = false
        val addDepsMagic = new AddDeps
          with IncludeSparkContext
          with IncludeInterpreter
          with IncludeOutputStream
          with IncludeDependencyDownloader
          with ArgumentParsingSupport
        {
          override val sparkContext: SparkContext = mock[SparkContext]
          override val interpreter: Interpreter = mock[Interpreter]
          override val dependencyDownloader: DependencyDownloader =
            mock[DependencyDownloader]
          override val outputStream: OutputStream = byteArrayOutputStream

          override def printHelp(outputStream: OutputStream, usage: String): Unit =
            printHelpWasRun = true
        }

        val expected = MagicOutput()
        val actual = addDepsMagic.executeLine("notvalid")

        printHelpWasRun should be (true)
        actual should be (expected)
      }

      it("should set the retrievals transitive to true if provided") {
        val mockDependencyDownloader = mock[DependencyDownloader]
        doReturn(Nil).when(mockDependencyDownloader).retrieve(
          anyString(), anyString(), anyString(), anyBoolean())

        val addDepsMagic = new AddDeps
          with IncludeSparkContext
          with IncludeInterpreter
          with IncludeOutputStream
          with IncludeDependencyDownloader
          with ArgumentParsingSupport
        {
          override val sparkContext: SparkContext = mock[SparkContext]
          override val interpreter: Interpreter = mock[Interpreter]
          override val dependencyDownloader: DependencyDownloader =
            mockDependencyDownloader
          override val outputStream: OutputStream = mock[OutputStream]
        }

        val expected = "com.ibm" :: "ignitio" :: "1.0" :: "--transitive" :: Nil
        addDepsMagic.executeLine(expected.mkString(" "))

        verify(mockDependencyDownloader).retrieve(
          expected(0), expected(1), expected(2), true)
      }

      it("should set the retrieval's transitive to false if not provided") {
        val mockDependencyDownloader = mock[DependencyDownloader]
        doReturn(Nil).when(mockDependencyDownloader).retrieve(
          anyString(), anyString(), anyString(), anyBoolean())

        val addDepsMagic = new AddDeps
          with IncludeSparkContext
          with IncludeInterpreter
          with IncludeOutputStream
          with IncludeDependencyDownloader
          with ArgumentParsingSupport
        {
          override val sparkContext: SparkContext = mock[SparkContext]
          override val interpreter: Interpreter = mock[Interpreter]
          override val dependencyDownloader: DependencyDownloader =
            mockDependencyDownloader
          override val outputStream: OutputStream = mock[OutputStream]
        }

        val expected = "com.ibm" :: "ignitio" :: "1.0" :: Nil
        addDepsMagic.executeLine(expected.mkString(" "))

        verify(mockDependencyDownloader).retrieve(
          expected(0), expected(1), expected(2), false)
      }

      it("should add retrieved artifacts to the interpreter") {
        val mockDependencyDownloader = mock[DependencyDownloader]
        doReturn(Nil).when(mockDependencyDownloader).retrieve(
          anyString(), anyString(), anyString(), anyBoolean())
        val mockInterpreter = mock[Interpreter]

        val addDepsMagic = new AddDeps
          with IncludeSparkContext
          with IncludeInterpreter
          with IncludeOutputStream
          with IncludeDependencyDownloader
          with ArgumentParsingSupport
        {
          override val sparkContext: SparkContext = mock[SparkContext]
          override val interpreter: Interpreter = mockInterpreter
          override val dependencyDownloader: DependencyDownloader =
            mockDependencyDownloader
          override val outputStream: OutputStream = mock[OutputStream]
        }

        val expected = "com.ibm" :: "ignitio" :: "1.0" :: Nil
        addDepsMagic.executeLine(expected.mkString(" "))

        verify(mockInterpreter).addJars(any[URL])
      }

      it("should add retrieved artifacts to the spark context") {
        val mockDependencyDownloader = mock[DependencyDownloader]
        val fakeUrl = new URL("file:/foo")
        doReturn(fakeUrl :: fakeUrl :: fakeUrl :: Nil)
          .when(mockDependencyDownloader).retrieve(
            anyString(), anyString(), anyString(), anyBoolean()
          )
        val mockSparkContext = mock[SparkContext]

        val addDepsMagic = new AddDeps
          with IncludeSparkContext
          with IncludeInterpreter
          with IncludeOutputStream
          with IncludeDependencyDownloader
          with ArgumentParsingSupport
        {
          override val sparkContext: SparkContext = mockSparkContext
          override val interpreter: Interpreter = mock[Interpreter]
          override val dependencyDownloader: DependencyDownloader =
            mockDependencyDownloader
          override val outputStream: OutputStream = mock[OutputStream]
        }

        val expected = "com.ibm" :: "ignitio" :: "1.0" :: Nil
        addDepsMagic.executeLine(expected.mkString(" "))

        verify(mockSparkContext, times(3)).addJar(anyString())
      }

      it("should return an empty MagicOutput if the input is invalid") {
        val mockDependencyDownloader = mock[DependencyDownloader]
        doReturn(Nil).when(mockDependencyDownloader).retrieve(
          anyString(), anyString(), anyString(), anyBoolean())

        val addDepsMagic = new AddDeps
          with IncludeSparkContext
          with IncludeInterpreter
          with IncludeOutputStream
          with IncludeDependencyDownloader
          with ArgumentParsingSupport
        {
          override val sparkContext: SparkContext = mock[SparkContext]
          override val interpreter: Interpreter = mock[Interpreter]
          override val dependencyDownloader: DependencyDownloader =
            mockDependencyDownloader
          override val outputStream: OutputStream = mock[OutputStream]
        }

        val expected = MagicOutput()
        val actual = addDepsMagic.executeLine((Nil).mkString(" "))
        actual should be (expected)
      }

      it("should return an empty MagicOutput on success") {
        val mockDependencyDownloader = mock[DependencyDownloader]
        doReturn(Nil).when(mockDependencyDownloader).retrieve(
          anyString(), anyString(), anyString(), anyBoolean())

        val addDepsMagic = new AddDeps
          with IncludeSparkContext
          with IncludeInterpreter
          with IncludeOutputStream
          with IncludeDependencyDownloader
          with ArgumentParsingSupport
        {
          override val sparkContext: SparkContext = mock[SparkContext]
          override val interpreter: Interpreter = mock[Interpreter]
          override val dependencyDownloader: DependencyDownloader =
            mockDependencyDownloader
          override val outputStream: OutputStream = mock[OutputStream]
        }

        val expected = MagicOutput()
        val actual = addDepsMagic.executeLine((
          "com.ibm" :: "ignitio" :: "1.0" :: Nil).mkString(" "))
        actual should be (expected)
      }
    }
  }
}
