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

package integration.interpreter.scala

import java.util
import java.io.ByteArrayOutputStream

import jupyter.{Displayer, Displayers, MIMETypes}
import org.apache.toree.annotations.SbtForked
import org.apache.toree.global.StreamState
import org.apache.toree.interpreter.Interpreter
import org.apache.toree.interpreter.Results.Success
import org.apache.toree.kernel.api.{DisplayMethodsLike, KernelLike}
import org.apache.toree.kernel.interpreter.scala.ScalaInterpreter
import org.mockito.Mockito.doReturn
import org.scalatest.{BeforeAndAfter, FunSpec, Ignore, Matchers}
import org.scalatestplus.mockito.MockitoSugar

import scala.util.Random

@SbtForked
@Ignore
class JVMReprSpec extends FunSpec with Matchers with MockitoSugar with BeforeAndAfter {

  private val outputResult = new ByteArrayOutputStream()
  private var interpreter: Interpreter = _

  before {
    val mockKernel = mock[KernelLike]
    val mockDisplayMethods = mock[DisplayMethodsLike]
    doReturn(mockDisplayMethods).when(mockKernel).display

    interpreter = new ScalaInterpreter().init(mockKernel)

    StreamState.setStreams(outputStream = outputResult)
  }

  after {
    interpreter.stop()
    outputResult.reset()
  }

  describe("ScalaInterpreter") {
    describe("#interpret") {
      it("should display Scala int as a text representation") {
        val (result, outputOrError) = interpreter.interpret("val a = 12")

        result should be(Success)
        outputOrError.isLeft should be(true)
        outputOrError.left.get should be(Map(MIMETypes.TEXT -> "12"))
      }

      it("should display Scala Some(str) as a text representation") {
        val (result, outputOrError) = interpreter.interpret("""val a = Some("str")""")

        result should be(Success)
        outputOrError.isLeft should be(true)
        outputOrError.left.get should be(Map(MIMETypes.TEXT -> "Some(str)"))
      }

      ignore("should use the Jupyter REPR API for display representation") {
        Displayers.register(classOf[DisplayerTest], new Displayer[DisplayerTest] {
          override def display(t: DisplayerTest): util.Map[String, String] = {
            val output = new util.HashMap[String, String]()
            output.put("text/plain", s"test object: ${t.id}")
            output.put("application/json", s"""{"id": ${t.id}""")
            output
          }
        })

        val inst = DisplayerTest()
        interpreter.bind("inst", classOf[DisplayerTest].getName, inst, List())

        val (result, outputOrError) = interpreter.interpret("""inst""")

        result should be(Success)
        outputOrError.isLeft should be(true)
        outputOrError.left.get should be(Map(
          MIMETypes.TEXT -> s"test object: ${inst.id}",
          "application/json" -> s"""{"id": ${inst.id}"""
        ))
      }
    }
  }
}

case class DisplayerTest(id: Long = new Random().nextLong())
