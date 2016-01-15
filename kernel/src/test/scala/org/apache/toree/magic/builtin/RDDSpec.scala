/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.toree.magic.builtin

import org.apache.toree.interpreter.Results.Result
import org.apache.toree.interpreter.{Results, ExecuteAborted, ExecuteError, Interpreter}
import org.apache.toree.kernel.protocol.v5.MIMEType
import org.apache.toree.magic.dependencies.{IncludeKernelInterpreter, IncludeInterpreter}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.StructType
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import play.api.libs.json.Json

class RDDSpec extends FunSpec with Matchers with MockitoSugar with BeforeAndAfter {

  val resOutput = "res1: org.apache.spark.sql.SchemaRDD ="

  val mockInterpreter = mock[Interpreter]
  val mockDataFrame = mock[DataFrame]
  val mockRdd = mock[org.apache.spark.rdd.RDD[Any]]
  val mockStruct = mock[StructType]
  val columns = Seq("foo", "bar").toArray
  val rows = Array( Array("a", "b"), Array("c", "d") )

  doReturn(mockStruct).when(mockDataFrame).schema
  doReturn(columns).when(mockStruct).fieldNames
  doReturn(mockRdd).when(mockDataFrame).map(any())(any())
  doReturn(rows).when(mockRdd).take(anyInt())

  val rddMagic = new RDD with IncludeKernelInterpreter {
    override val kernelInterpreter: Interpreter = mockInterpreter
  }

  before {
    doReturn(Some("someRDD")).when(mockInterpreter).lastExecutionVariableName
    doReturn(Some(mockDataFrame)).when(mockInterpreter).read(anyString())
    doReturn((Results.Success, Left(resOutput)))
      .when(mockInterpreter).interpret(anyString(), anyBoolean())
  }

  describe("RDD") {
    describe("#execute") {
      it("should return valid JSON when the executed code evaluates to a " +
         "SchemaRDD") {
        val magicOutput = rddMagic.execute("schemaRDD")
        magicOutput.contains(MIMEType.ApplicationJson) should be (true)
        Json.parse(magicOutput(MIMEType.ApplicationJson))
      }

      it("should return normally when the executed code does not evaluate to " +
         "a SchemaRDD") {
        doReturn((mock[Result], Left("foo"))).when(mockInterpreter)
          .interpret(anyString(), anyBoolean())
        val magicOutput = rddMagic.execute("")
        magicOutput.contains(MIMEType.PlainText) should be (true)
      }

      it("should return error message when the interpreter does not return " +
         "SchemaRDD as expected") {
        doReturn(Some("foo")).when(mockInterpreter).read(anyString())
        val magicOutput = rddMagic.execute("")
        magicOutput.contains(MIMEType.PlainText) should be (true)
      }

      it("should throw a Throwable if the interpreter returns an ExecuteError"){
        val expected = "some error message"
        val mockExecuteError = mock[ExecuteError]
        doReturn(expected).when(mockExecuteError).value

        doReturn((mock[Result], Right(mockExecuteError))).when(mockInterpreter)
          .interpret(anyString(), anyBoolean())
        val actual = {
          val exception = intercept[Throwable] {
            rddMagic.execute("")
          }
          exception.getLocalizedMessage
        }

        actual should be (expected)
      }

      it("should throw a Throwable if the interpreter returns an " +
         "ExecuteAborted") {
        val expected = "RDD magic aborted!"
        val mockExecuteAborted = mock[ExecuteAborted]

        doReturn((mock[Result], Right(mockExecuteAborted)))
          .when(mockInterpreter).interpret(anyString(), anyBoolean())
        val actual = {
          val exception = intercept[Throwable] {
            rddMagic.execute("")
          }
          exception.getLocalizedMessage
        }

        actual should be (expected)
      }
    }
  }
}