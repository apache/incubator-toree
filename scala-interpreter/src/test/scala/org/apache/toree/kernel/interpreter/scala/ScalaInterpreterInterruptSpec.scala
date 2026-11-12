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

import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.toree.kernel.api.KernelLike
import org.apache.toree.utils.{TaskAbortedException, TaskManager}
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.{doThrow, never, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class ScalaInterpreterInterruptSpec extends AnyFunSpec with Matchers
  with MockitoSugar with ScalaFutures with BeforeAndAfterEach
{
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(10, Seconds)),
    interval = scaled(Span(5, Milliseconds))
  )

  private var release: AtomicBoolean = _
  private var mockKernel: KernelLike = _

  override def beforeEach(): Unit = {
    release = new AtomicBoolean(false)
    mockKernel = mock[KernelLike]
  }

  override def afterEach(): Unit = release.set(true)

  /** ScalaInterpreter with a fake Spark lookup; no REPL is started. */
  private class TestInterpreter(spark: Option[SparkContext])
    extends ScalaInterpreter
  {
    _kernel = mockKernel
    taskManager = new TaskManager()
    taskManager.start()

    override protected def activeSparkContext: Option[SparkContext] = spark

    /** Runs a task that ignores interrupts, like a CPU-bound cell. */
    def startStuckTask(): Future[Unit] = {
      val f = taskManager.add {
        while (!release.get()) {
          try Thread.sleep(5) catch { case _: InterruptedException => }
        }
      }
      while (!taskManager.isExecutingTask) Thread.sleep(1)
      f
    }

    /** Exposes the per-cell Spark job wrapper used by interpretAddTask. */
    def runCell[T](body: => T): T = withInterruptibleSparkJobs(body)
  }

  private def assertAborted(f: Future[_]): Unit =
    whenReady(f.failed) { ex =>
      ex shouldBe an [ExecutionException]
      ex.getCause shouldBe a [TaskAbortedException]
    }

  describe("ScalaInterpreter") {
    describe("#interrupt") {
      it("should not create a SparkContext when Spark is not running") {
        val interpreter = new TestInterpreter(spark = None)
        val stuck = interpreter.startStuckTask()

        interpreter.interrupt()

        verify(mockKernel, never()).sparkContext
        verify(mockKernel, never()).sparkSession
        assertAborted(stuck)
      }

      it("should cancel jobs on the running SparkContext") {
        val sc = mock[SparkContext]
        val interpreter = new TestInterpreter(spark = Some(sc))
        val stuck = interpreter.startStuckTask()

        interpreter.interrupt()

        verify(sc).cancelAllJobs()
        verify(mockKernel, never()).sparkContext
        assertAborted(stuck)
      }

      it("should still abort the running cell when cancelling jobs fails") {
        val sc = mock[SparkContext]
        doThrow(new IllegalStateException("cancel failed")).when(sc).cancelAllJobs()
        val interpreter = new TestInterpreter(spark = Some(sc))
        val stuck = interpreter.startStuckTask()

        noException should be thrownBy interpreter.interrupt()

        assertAborted(stuck)
      }
    }

    describe("#withInterruptibleSparkJobs") {
      def sparkWithConf(conf: SparkConf): SparkContext = {
        val sc = mock[SparkContext]
        when(sc.getConf).thenReturn(conf)
        sc
      }

      it("should make the cell's Spark jobs interruptible on cancel") {
        val sc = sparkWithConf(new SparkConf(false))
        val interpreter = new TestInterpreter(spark = Some(sc))

        interpreter.runCell { "result" } should be ("result")

        verify(sc).setLocalProperty("spark.job.interruptOnCancel", "true")
      }

      it("should leave jobs alone when disabled via spark.toree.interruptTasksOnCancel") {
        val sc = sparkWithConf(
          new SparkConf(false).set("spark.toree.interruptTasksOnCancel", "false"))
        val interpreter = new TestInterpreter(spark = Some(sc))

        interpreter.runCell { "result" } should be ("result")

        verify(sc, never()).setLocalProperty(anyString(), anyString())
      }

      it("should just run the cell when Spark is not running") {
        val interpreter = new TestInterpreter(spark = None)

        interpreter.runCell { "result" } should be ("result")

        verify(mockKernel, never()).sparkContext
      }
    }
  }
}
