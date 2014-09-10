package com.ibm.spark.interpreter

import java.io.{File, InputStream, OutputStream}
import java.net.URL

import org.apache.spark.{SparkConf, HttpServer}
import com.ibm.spark.utils.TaskManager
import org.apache.spark.repl.SparkIMain
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import scala.tools.nsc.{interpreter, Settings}

import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, Matchers, FunSpec}

import org.mockito.Mockito._
import org.mockito.Matchers._
import org.mockito.AdditionalAnswers._

import scala.tools.nsc.backend.JavaPlatform
import scala.tools.nsc.interpreter.JPrintWriter
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.util.MergedClassPath
import scala.tools.nsc.interpreter._

class ScalaInterpreterSpec extends FunSpec
  with Matchers with MockitoSugar with BeforeAndAfter
{
  private var interpreter: ScalaInterpreter               = _
  private var interpreterNoPrintStreams: ScalaInterpreter = _
  private var mockSparkIMain: SparkIMain                  = _
  private var mockTaskManager: TaskManager                = _
  private var mockSettings: Settings                      = _

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

    interpreterNoPrintStreams =
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
        override def updatePrintStreams(
          in: InputStream,
          out: OutputStream,
          err: OutputStream
        ): Unit = {}
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
        interpreterNoPrintStreams.start()

        interpreterNoPrintStreams.interrupt()

        verify(mockTaskManager).restart()
      }
    }

    describe("#interpret") {
      it("should fail if not started") {
        intercept[IllegalArgumentException] {
          interpreter.interpret("val x = 3")
        }
      }

      /*it("should add a new task to the task manager") {
        interpreterNoPrintStreams.start()

        interpreterNoPrintStreams.interpret("val x = 3")

        verify(mockTaskManager).add({ IR.Success })
      }*/
    }

    describe("#start") {
      it("should initialize the task manager") {
        interpreterNoPrintStreams.start()

        verify(mockTaskManager).start()
      }

      it("should initialize the SparkIMain") {
        interpreterNoPrintStreams.start()

        verify(mockSparkIMain).initializeSynchronous()
      }

      // TODO: Figure out how to trigger sparkIMain.beQuietDuring { ... }
      /*it("should add an import for SparkContext._") {
        interpreterNoPrintStreams.start()

        verify(mockSparkIMain).addImports("org.apache.spark.SparkContext._")
      }*/
    }

    describe("#stop") {
      describe("when interpreter already started") {
        it("should stop the task manager") {
          interpreterNoPrintStreams.start()
          interpreterNoPrintStreams.stop()

          verify(mockTaskManager).stop()
        }

        it("should stop the SparkIMain") {
          interpreterNoPrintStreams.start()
          interpreterNoPrintStreams.stop()

          verify(mockSparkIMain).close()
        }
      }
    }

    describe("#updatePrintStreams") {
      // TODO: Figure out how to trigger sparkIMain.beQuietDuring { ... }
    }

    describe("#classServerUri") {
      it("should fail a require if the interpreter is not started") {
        intercept[IllegalArgumentException] {
          interpreter.classServerURI
        }
      }

      // TODO: Find better way to test this
      it("should invoke the underlying SparkIMain implementation") {
        // Using hack to access private class
        val securityManagerClass =
          java.lang.Class.forName("org.apache.spark.SecurityManager")
        val httpServerClass =
          java.lang.Class.forName("org.apache.spark.HttpServer")
        val httpServerConstructor = httpServerClass.getDeclaredConstructor(
          classOf[File], securityManagerClass)
        val httpServer = httpServerConstructor.newInstance(null, null)

        // Return the server instance (cannot mock a private class)
        // NOTE: Can mock the class through reflection, but cannot verify
        //       a method was called on it since treated as type Any
        //val mockHttpServer = org.mockito.Mockito.mock(httpServerClass)
        doReturn(httpServer).when(mockSparkIMain).classServer

        interpreterNoPrintStreams.start()

        // Not going to dig so deeply that we actually start a web server for
        // this to work... just throwing this specific exception proves that
        // we have called the uri method of the server
        try {
          interpreterNoPrintStreams.classServerURI
        } catch {
          // Have to catch this way because... of course... the exception is
          // also private
          case ex: Throwable  =>
            ex.getClass.getName should be ("org.apache.spark.ServerStateException")
        }
      }
    }

    describe("#doQuietly") {
      it("should fail a require if the interpreter is not started") {
        intercept[IllegalArgumentException] {
          interpreter.doQuietly {}
        }
      }

      // TODO: Figure out how to verify sparkIMain.beQuietDuring { ... }
      /*it("should invoke the underlying SparkIMain implementation") {
        interpreterNoPrintStreams.start()
        interpreterNoPrintStreams.doQuietly {}

        verify(mockSparkIMain).beQuietDuring(any[IR.Result])
      }*/
    }

    describe("#bind") {
      it("should fail a require if the interpreter is not started") {
        intercept[IllegalArgumentException] {
          interpreter.bind("", "", null, null)
        }
      }

      it("should invoke the underlying SparkIMain implementation") {
        interpreterNoPrintStreams.start()
        interpreterNoPrintStreams.bind("", "", null, null)

        verify(mockSparkIMain).bind(
          anyString(), anyString(), any[Any], any[List[String]])
      }
    }
  }
}
