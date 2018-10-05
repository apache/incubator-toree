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

package org.apache.toree.kernel.interpreter.scala

import java.io.{InputStream, OutputStream}
import java.net.{URL, URLClassLoader}
import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.apache.toree.interpreter.Results.Result
import org.apache.toree.interpreter._
import org.apache.toree.kernel.api.KernelLike
import org.apache.toree.utils.TaskManager
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import scala.concurrent.Future
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.{IMain, IR, JPrintWriter}
import scala.tools.nsc.util.ClassPath

class ScalaInterpreterSpec extends FunSpec
  with Matchers with MockitoSugar with BeforeAndAfter
{
  private var interpreter: ScalaInterpreter               = _
  private var interpreterNoPrintStreams: ScalaInterpreter = _
  private var mockSparkIMain: IMain                  = _
  private var mockTaskManager: TaskManager                = _
  private var mockSettings: Settings                      = _
  private var mockKernel: KernelLike                      = _
  private var mockSparkSession: SparkSession              = _
  private var mockSparkContext: SparkContext              = _

  trait StubbedUpdatePrintStreams extends Interpreter {
    override def updatePrintStreams(
      in: InputStream,
      out: OutputStream,
      err: OutputStream
    ): Unit = {}
  }

  trait SingleLineInterpretLineRec extends StubbedStartInterpreter {
    protected def interpretRec(lines: List[String], silent: Boolean, results: (Result, Either[ExecuteOutput, ExecuteFailure])): (Result, Either[ExecuteOutput, ExecuteFailure]) =
      interpretBlock(lines.mkString("\n"))
  }

  trait StubbedInterpretAddTask extends StubbedStartInterpreter {
    override protected def interpretAddTask(code: String, silent: Boolean) =
      mock[Future[IR.Result]]
  }

  trait StubbedInterpretMapToCustomResult extends StubbedStartInterpreter {
    override protected def interpretMapToCustomResult(future: Future[IR.Result]) =
      mock[Future[Results.Result with Product with Serializable]]
  }

  trait StubbedInterpretMapToResultAndOutput extends StubbedStartInterpreter {
    override protected def interpretMapToResultAndOutput(future: Future[Results.Result]) =
      mock[Future[(Results.Result, Either[Map[String, String], ExecuteError])]]
  }

  trait StubbedInterpretMapToResultAndExecuteInfo extends StubbedStartInterpreter {
    protected def interpretMapToResultAndExecuteInfo(future: Future[(Results.Result, String)]) =
      mock[Future[(
        Results.Result with Product with Serializable,
        Either[ExecuteOutput, ExecuteFailure] with Product with Serializable
      )]]
  }

  trait StubbedInterpretConstructExecuteError extends StubbedStartInterpreter {
    protected def interpretConstructExecuteError(value: Option[AnyRef], output: String) =
      mock[ExecuteError]
  }

  class StubbedStartInterpreter
    extends ScalaInterpreter
  {

    override protected def newIMain(settings: Settings, out: JPrintWriter): IMain = mockSparkIMain
    override def newTaskManager(): TaskManager = mockTaskManager
    override def newSettings(args: List[String]): Settings = mockSettings

    override protected def kernel: KernelLike = mockKernel

    // mocking out these
    override protected def reinitializeSymbols(): Unit = {}
    override protected def refreshDefinitions(): Unit = {}

    // Stubbed out (not testing this)
  }

  before {
    mockSparkIMain  = mock[IMain]

    mockTaskManager = mock[TaskManager]

    val mockSettingsClasspath = mock[Settings#PathSetting]
    doNothing().when(mockSettingsClasspath).value_=(any[Settings#PathSetting#T])

    mockSettings    = mock[Settings]
    doReturn(mockSettingsClasspath).when(mockSettings).classpath
    doNothing().when(mockSettings).embeddedDefaults(any[ClassLoader])

    mockKernel = mock[KernelLike]
    mockSparkSession = mock[SparkSession]
    mockSparkContext = mock[SparkContext]
    doReturn(mockSparkSession).when(mockKernel).sparkSession
    doReturn(mockSparkContext).when(mockKernel).sparkContext

    interpreter = new StubbedStartInterpreter

    interpreterNoPrintStreams =
      new StubbedStartInterpreter with StubbedUpdatePrintStreams
  }

  after {
    mockSparkIMain  = null
    mockTaskManager = null
    mockSettings    = null
    mockKernel      = null
    mockSparkSession = null
    mockSparkContext = null
    interpreter     = null
  }

  describe("ScalaInterpreter") {
    describe("#addJars") {
      // Mocked test ignored.
      ignore("should add each jar URL to the runtime classloader") {
        // Needed to access runtimeClassloader method
//        import scala.language.reflectiveCalls

        // Create a new interpreter exposing the internal runtime classloader
        val itInterpreter = new StubbedStartInterpreter {
          // Expose the runtime classloader


          def runtimeClassloader = _runtimeClassloader

        }

        val url = new URL("file://expected")
        itInterpreter.start()
        itInterpreter.addJars(url)

//        itInterpreter.runtimeClassloader
        val cl = itInterpreter.runtimeClassloader
//        cl.getURLs should contain (url)
        itInterpreter.stop()
      }

      it("should add each jar URL to the interpreter classpath") {
        val url = new URL("file://expected")
        interpreter.start()
        interpreter.addJars(url)
      }
    }

    describe("#buildClasspath") {
      it("should return classpath based on classloader hierarchy") {
        // Needed to access runtimeClassloader method
//        import scala.language.reflectiveCalls

        // Create a new interpreter exposing the internal runtime classloader
        val itInterpreter = new StubbedStartInterpreter

        val parentUrls = Array(
          new URL("file:/some/dir/a.jar"),
          new URL("file:/some/dir/b.jar"),
          new URL("file:/some/dir/c.jar")
        )

        val theParentClassloader = new URLClassLoader(parentUrls, null)

        val urls = Array(
          new URL("file:/some/dir/1.jar"),
          new URL("file:/some/dir/2.jar"),
          new URL("file:/some/dir/3.jar")
        )

        val theClassloader = new URLClassLoader(urls, theParentClassloader)

        val expected = ClassPath.join((parentUrls ++ urls).map(_.toString) :_*)

        itInterpreter.buildClasspath(theClassloader) should be(expected)
      }
    }

    describe("#interrupt") {
      it("should fail a require if the interpreter is not started") {
        intercept[IllegalArgumentException] {
          interpreter.interrupt()
        }
      }

      it("should call restart() on the task manager and cancelAllJobs on SparkContext") {
        interpreterNoPrintStreams.start()

        // cancelAllJobs still leaves the task running
        doReturn(true).when(mockTaskManager).isExecutingTask

        interpreterNoPrintStreams.interrupt()

        // restart is called
        verify(mockSparkContext).cancelAllJobs()
        verify(mockTaskManager).restart()
      }

      it("should only call cancelAllJobs and not restart if task execution ends") {
        interpreterNoPrintStreams.start()

        interpreterNoPrintStreams.interrupt()

        // Spark jobs are cancelled
        verify(mockSparkContext).cancelAllJobs()
        // The task manager is not executing, so it is not restarted
        verify(mockTaskManager, atLeastOnce).isExecutingTask
        verifyNoMoreInteractions(mockSparkContext)
      }
    }

    // TODO: Provide testing for the helper functions that return various
    //       mapped futures -- this was too difficult for me to figure out
    //       in a short amount of time
    describe("#interpret") {
      it("should fail if not started") {
        intercept[IllegalArgumentException] {
          interpreter.interpret("val x = 3")
        }
      }

      it("should add a new task to the task manager") {
        var taskManagerAddCalled = false
        val itInterpreter =
          new StubbedStartInterpreter
          with SingleLineInterpretLineRec
          with StubbedUpdatePrintStreams
          //with StubbedInterpretAddTask
          with StubbedInterpretMapToCustomResult
          with StubbedInterpretMapToResultAndOutput
          with StubbedInterpretMapToResultAndExecuteInfo
          with StubbedInterpretConstructExecuteError
          with TaskManagerProducerLike
        {
          // Must override this way since cannot figure out the signature
          // to verify this as a mock
          override def newTaskManager(): TaskManager = new TaskManager {
            override def add[T](taskFunction: => T): Future[T] = {
              taskManagerAddCalled = true
              mock[TaskManager].add(taskFunction)
            }
          }
        }

        itInterpreter.start()

        itInterpreter.interpret("val x = 3")

        taskManagerAddCalled should be (true)
      }
    }

    describe("#start") {
      it("should initialize the task manager") {
        interpreterNoPrintStreams.start()

        verify(mockTaskManager).start()
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

//    describe("#classServerUri") {
//      it("should fail a require if the interpreter is not started") {
//        intercept[IllegalArgumentException] {
//          interpreter.classServerURI
//        }
//      }

//       TODO: Find better way to test this
//      it("should invoke the underlying SparkIMain implementation") {
        // Using hack to access private class
//        val securityManagerClass =
//          java.lang.Class.forName("org.apache.spark.SecurityManager")
//        val httpServerClass =
//          java.lang.Class.forName("org.apache.spark.HttpServer")
//        val httpServerConstructor = httpServerClass.getDeclaredConstructor(
//          classOf[SparkConf], classOf[File], securityManagerClass, classOf[Int],
//          classOf[String])
//        val httpServer = httpServerConstructor.newInstance(
//          null, null, null, 0: java.lang.Integer, "")
//
//        // Return the server instance (cannot mock a private class)
//        // NOTE: Can mock the class through reflection, but cannot verify
//        //       a method was called on it since treated as type Any
//        //val mockHttpServer = org.mockito.Mockito.mock(httpServerClass)
//        doAnswer(new Answer[String] {
//          override def answer(invocation: InvocationOnMock): String = {
//            val exceptionClass =
//              java.lang.Class.forName("org.apache.spark.ServerStateException")
//            val exception = exceptionClass
//              .getConstructor(classOf[String])
//              .newInstance("")
//              .asInstanceOf[Exception]
//            throw exception
//          }
//        }
//        ).when(mockSparkIMain)

//        interpreterNoPrintStreams.start()

        // Not going to dig so deeply that we actually start a web server for
        // this to work... just throwing this specific exception proves that
        // we have called the uri method of the server
//        try {
//          interpreterNoPrintStreams.classServerURI
//          fail()
//        } catch {
//          // Have to catch this way because... of course... the exception is
//          // also private
//          case ex: Throwable  =>
//            ex.getClass.getName should be ("org.apache.spark.ServerStateException")
//        }
//      }
//    }

    describe("#read") {
      it("should fail a require if the interpreter is not started") {
        intercept[IllegalArgumentException] {
          interpreter.read("someVariable")
        }
      }

      it("should execute the underlying eval method") {
        interpreter.start()
        interpreter.read("someVariable")

        verify(mockSparkIMain).eval(anyString())
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

      // TODO: Re-enable tests since we've commented this one out.
//      it("should invoke the underlying SparkIMain implementation") {
//        interpreterNoPrintStreams.start()
//        interpreterNoPrintStreams.bind("", "", null, null)
//
//        verify(mockSparkIMain).bind(
//          anyString(), anyString(), any[Any], any[List[String]])
//      }
    }

    describe("#prepareResult") {
      it("should truncate result of res result") {
        interpreter.start()
        doReturn(38).when(mockSparkIMain).eval("i")
        doReturn("ABC").when(mockSparkIMain).eval("s")
        doReturn("abc").when(mockSparkIMain).eval("res4")

        //  Results that match  ==> Result, Definitions, Text
        //  val i: Int = 38 ==> i: Int = 38
        interpreter.prepareResult("i: Int = 38") should be((Some("38"), Some("i = 38\n"), None))
        interpreter.prepareResult("i: Int = 38",true) should be((Some("i: Int = 38\n"), Some("i: Int = 38\n"), None))
        // val s = "ABC" ==> s: String = ABC
        interpreter.prepareResult("s: String = ABC") should be((Some("ABC"), Some("s = ABC\n"), None))
        interpreter.prepareResult("s: String = ABC",true) should be((Some("s: String = ABC\n"), Some("s: String = ABC\n"), None))
        // resN results are suppressed
        interpreter.prepareResult("res4: String = abc") should be((Some("abc"), None, None))
        interpreter.prepareResult("res4: String = abc",true) should be((Some("String = abc\n"), None, None))
        // missing variables are None, unmatched lines are returned in text
        interpreter.prepareResult("res123") should be((None, None, Some("res123\n")))
        interpreter.prepareResult("res123: Int = 38") should be((None, None, Some("res123: Int = 38\n")))

        interpreter.stop()
      }

      it("should properly handle higher order functions") {
        interpreter.start()
        doReturn("myFunction: (x: Int, foo: Int => Int)Int").when(mockSparkIMain).eval("myFunction")

        //  Results that match
        interpreter.prepareResult("myFunction: (x: Int, foo: Int => Int)Int") should be(
          (None,
            Some("myFunction: (x: Int, foo: Int => Int)Int\n"),
            None))


        interpreter.stop()

      }

      it("should truncate res results that have tuple values") {
        //val t: (String, Int) = ("hello",1)  ==>  t: (String, Int) = (hello,1)
        interpreter.start()
        doReturn("(hello, 1)").when(mockSparkIMain).eval("res0")

        interpreter.prepareResult("res0: (String, Int) = (hello,1)") should be((Some("(hello,1)"), None, None))

        interpreter.stop()
      }

      it("should truncate res results that have parameterized types") {
        interpreter.start()
        doReturn(scala.Tuple2).when(mockSparkIMain).eval("res0")

        interpreter.prepareResult(
          "res0: Class[_ <: (String, Int)] = class scala.Tuple2", noTruncate = true
        ) should be((Some(scala.Tuple2), None, None))

        interpreter.stop()
      }

    }
  }
}
