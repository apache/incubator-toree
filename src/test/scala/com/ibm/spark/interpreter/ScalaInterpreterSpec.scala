package com.ibm.spark.interpreter

import java.io.{InputStream, OutputStream}
import java.net.URL

import com.ibm.spark.utils.TaskManager
import org.apache.spark.repl.SparkIMain
import scala.tools.nsc.{interpreter, Settings}

import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, Matchers, FunSpec}

import org.mockito.Mockito._
import org.mockito.Matchers._

import scala.tools.nsc.backend.JavaPlatform
import scala.tools.nsc.interpreter.JPrintWriter
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.util.MergedClassPath

class ScalaInterpreterSpec extends FunSpec
  with Matchers with MockitoSugar with BeforeAndAfter
{
  private var interpreter: ScalaInterpreter = _
  private var mockSparkIMain: SparkIMain    = _
  private var mockTaskManager: TaskManager  = _
  private var mockSettings: Settings        = _

  before {
    mockSparkIMain  = mock[SparkIMain]
    mockTaskManager = mock[TaskManager]

    val mockSettingsClasspath = mock[Settings#PathSetting]
    doNothing().when(mockSettingsClasspath).value_=(any[Settings#PathSetting#T])

    mockSettings    = mock[Settings]
    doReturn(mockSettingsClasspath).when(mockSettings).classpath
    doNothing().when(mockSettings).embeddedDefaults(any[ClassLoader])

    interpreter =
      new ScalaInterpreter(mock[List[String]], mock[OutputStream])
        with SparkIMainProducerLike
        with TaskManagerProducerLike
        with SettingsProducerLike
      {
        override def newSparkIMain(
          settings: Settings, out: JPrintWriter
        ): SparkIMain = mockSparkIMain
        override def newTaskManager(): TaskManager = mockTaskManager
        override def newSettings(args: List[String]): Settings = mockSettings

        // Stubbed out (not testing this)
        override protected def updateCompilerClassPath(jars: URL*): Unit = {}

        override protected def reinitializeSymbols(): Unit = {}
      }
  }

  after {
    mockSparkIMain  = null
    mockTaskManager = null
    mockSettings    = null
    interpreter     = null
  }

  describe("ScalaInterpreter") {
    describe("#addJars") {
      it("should add each jar URL to the runtime classloader") {
        // Needed to access runtimeClassloader method
        import scala.language.reflectiveCalls

        // Create a new interpreter exposing the internal runtime classloader
        val itInterpreter =
          new ScalaInterpreter(mock[List[String]], mock[OutputStream])
            with SparkIMainProducerLike
            with TaskManagerProducerLike
            with SettingsProducerLike
          {
            override def newSparkIMain(
                                        settings: Settings, out: JPrintWriter
                                        ): SparkIMain = mockSparkIMain
            override def newTaskManager(): TaskManager = mockTaskManager
            override def newSettings(args: List[String]): Settings = mockSettings

            // Stubbed out (not testing this)
            override protected def updateCompilerClassPath(jars: URL*): Unit = {}

            override protected def reinitializeSymbols(): Unit = {}

            // Expose the runtime classloader
            def runtimeClassloader = _runtimeClassloader
          }

        val url = new URL("file://expected")
        itInterpreter.addJars(url)

        itInterpreter.runtimeClassloader.getURLs should contain (url)
      }

      it("should add each jar URL to the interpreter classpath") {
        val url = new URL("file://expected")
        interpreter.addJars(url)
      }
    }

    describe("#interrupt") {
      it("should fail a require if the interpreter is not started") {
        intercept[IllegalArgumentException] {
          interpreter.interrupt()
        }
      }

      it("should call restart() on the task manager") {
        // Create a new interpreter with a stubbed out updatePrintStreams method
        val itInterpreter =
          new ScalaInterpreter(mock[List[String]], mock[OutputStream])
            with SparkIMainProducerLike
            with TaskManagerProducerLike
            with SettingsProducerLike
          {
            override def newSparkIMain(
              settings: Settings, out: JPrintWriter
            ): SparkIMain = mockSparkIMain
            override def newTaskManager(): TaskManager = mockTaskManager
            override def newSettings(args: List[String]): Settings = mockSettings

            // Stubbed out (not testing this)
            override protected def updateCompilerClassPath(jars: URL*): Unit = {}

            override def updatePrintStreams(
              in: InputStream,
              out: OutputStream,
              err: OutputStream
            ): Unit = {}
          }

        itInterpreter.start()

        itInterpreter.interrupt()

        verify(mockTaskManager).restart()
      }
    }

    describe("#interpret") {

    }

    describe("#start") {

    }

    describe("#stop") {

    }

    describe("#updatePrintStreams") {

    }

    describe("#classServerUri") {

    }

    describe("#doQuietly") {

    }

    describe("#bind") {

    }
  }
}
