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

package org.apache.toree.magic.builtin

import org.apache.toree.interpreter._
import org.apache.toree.kernel.protocol.v5.MIMEType
import org.apache.toree.magic.dependencies.IncludeKernelInterpreter
import org.apache.toree.utils.DataFrameConverter
import org.mockito.Matchers.{anyString, eq => mockEq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import scala.util.Success

class DataFrameSpec extends FunSpec with Matchers with MockitoSugar with BeforeAndAfter {

  def createMocks = {
    val interpreter = mock[Interpreter]
    val converter = mock[DataFrameConverter]
    val magic = new DataFrame with IncludeKernelInterpreter {
      override val kernelInterpreter: Interpreter = interpreter
    }
    magic.initMethod(converter)
    (magic, interpreter, converter)
  }

  describe("DataFrame") {
    describe("#execute") {
      it("should return a plain text error message on aborted execution"){
        val (magic, interpreter, _) = createMocks
        val message: Either[ExecuteOutput, ExecuteFailure] = Right(mock[ExecuteAborted])
        val code = "code"
        when(interpreter.interpret(code)).thenReturn((Results.Aborted, message))
        val output = magic.execute(code).asMap
        output.contains(MIMEType.PlainText) should be(true)
        output(MIMEType.PlainText) should be(DataFrameResponses.ErrorMessage(
          "html",
          DataFrameResponses.MagicAborted
        ))
      }

      it("should return a plain text error message on execution errors"){
        val (magic, interpreter, _) = createMocks
        val mockExecuteError = mock[ExecuteError]
        val mockError = "error"
        doReturn(mockError).when(mockExecuteError).value
        val message: Either[ExecuteOutput, ExecuteFailure] = Right(mockExecuteError)
        val code = "code"
        when(interpreter.interpret(code)).thenReturn((Results.Error, message))
        val output = magic.execute(code).asMap
        output.contains(MIMEType.PlainText) should be(true)
        output(MIMEType.PlainText) should be(DataFrameResponses.ErrorMessage(
          "html",
          mockError
        ))
      }

      it("should return a plain text message when there is no variable reference"){
        val (magic, interpreter, _) = createMocks
        val mockExecuteError = mock[ExecuteError]
        val mockError = "error"
        doReturn(mockError).when(mockExecuteError).value
        val message: Either[ExecuteOutput, ExecuteFailure] = Right(mockExecuteError )
        val code = "code"
        when(interpreter.interpret(code)).thenReturn((Results.Error, message))
        val output = magic.execute(code).asMap
        output.contains(MIMEType.PlainText) should be(true)
        output(MIMEType.PlainText) should be(DataFrameResponses.ErrorMessage(
          "html",
          mockError
        ))
      }

      it("should return a plain text message with help when there are no args"){
        val (magic, _, _) = createMocks
        val code = ""
        val output = magic.execute(code).asMap
        output.contains(MIMEType.PlainText) should be(true)
        output(MIMEType.PlainText).contains(DataFrameResponses.Usage) should be(true)
      }

      it("should return a json message when json is the selected output"){
        val (magic, interpreter, converter) = createMocks
        val outputText = "test output"
        val message: Either[ExecuteOutput, ExecuteFailure] = Left(Map("text/plain" -> outputText))
        val mockDataFrame = mock[org.apache.spark.sql.DataFrame]
        val variableName = "variable"
        val executeCode =s"""--output=json
                             |${variableName}
                  """.stripMargin
        when(interpreter.interpret(variableName)).thenReturn((Results.Success, message))
        doReturn(Some(variableName)).when(interpreter).lastExecutionVariableName
        doReturn(Some(mockDataFrame)).when(interpreter).read(variableName)
        doReturn(Success(outputText)).when(converter).convert(
          mockDataFrame,"json", 10
        )
        val output = magic.execute(executeCode).asMap
        output.contains(MIMEType.ApplicationJson) should be(true)
        output(MIMEType.ApplicationJson).contains(outputText) should be(true)
      }

      it("should return an html message when html is the selected output"){
        val (magic, interpreter, converter) = createMocks
        val outputText = "test output"
        val message: Either[ExecuteOutput, ExecuteFailure] = Left(Map("text/plain" -> outputText))
        val mockDataFrame = mock[org.apache.spark.sql.DataFrame]
        val variableName = "variable"
        val executeCode =s"""--output=html
                             |${variableName}
                  """.stripMargin
        when(interpreter.interpret(variableName)).thenReturn((Results.Success, message))
        doReturn(Some(variableName)).when(interpreter).lastExecutionVariableName
        doReturn(Some(mockDataFrame)).when(interpreter).read(variableName)
        doReturn(Success(outputText)).when(converter).convert(
          mockDataFrame,"html", 10
        )
        val output = magic.execute(executeCode).asMap
        output.contains(MIMEType.TextHtml) should be(true)
        output(MIMEType.TextHtml).contains(outputText) should be(true)
      }

      it("should return a csv message when csv is the selected output"){
        val (magic, interpreter, converter) = createMocks
        val outputText = "test output"
        val message: Either[ExecuteOutput, ExecuteFailure] = Left(Map("text/plain" -> outputText))
        val mockDataFrame = mock[org.apache.spark.sql.DataFrame]
        val variableName = "variable"
        val executeCode =s"""--output=csv
                             |${variableName}
                  """.stripMargin
        when(interpreter.interpret(variableName)).thenReturn((Results.Success, message))
        doReturn(Some(variableName)).when(interpreter).lastExecutionVariableName
        doReturn(Some(mockDataFrame)).when(interpreter).read(variableName)
        doReturn(Success(outputText)).when(converter).convert(
          mockDataFrame,"csv", 10
        )
        val output = magic.execute(executeCode).asMap
        output.contains(MIMEType.PlainText) should be(true)
        output(MIMEType.PlainText).contains(outputText) should be(true)
      }

      it("should pass the limit argument to the converter"){
        val (magic, interpreter, converter) = createMocks
        val outputText = "test output"
        val message: Either[ExecuteOutput, ExecuteFailure] = Left(Map("text/plain" -> outputText))
        val mockDataFrame = mock[org.apache.spark.sql.DataFrame]
        val variableName = "variable"
        val executeCode =s"""--output=html --limit=3
                             |${variableName}
                  """.stripMargin
        when(interpreter.interpret(variableName)).thenReturn((Results.Success, message))
        doReturn(Some(variableName)).when(interpreter).lastExecutionVariableName
        doReturn(Some(mockDataFrame)).when(interpreter).read(variableName)
        doReturn(Success(outputText)).when(converter).convert(
          mockDataFrame,"html", 3
        )
        magic.execute(executeCode)
        verify(converter).convert(any(), anyString(), mockEq(3))
      }

      it("should return a plain text message with help when the converter throws an exception"){
        val (magic, interpreter, converter) = createMocks
        val outputText = "test output"
        val message: Either[ExecuteOutput, ExecuteFailure] = Left(Map("text/plain" -> outputText))
        val mockDataFrame = mock[org.apache.spark.sql.DataFrame]
        val code = "variable"
        when(interpreter.interpret(code)).thenReturn((Results.Success, message))
        doReturn(Some(code)).when(interpreter).lastExecutionVariableName
        doReturn(Some(mockDataFrame)).when(interpreter).read(code)
        doThrow(new RuntimeException()).when(converter).convert(
          mockDataFrame,"html", 10
        )
        val output = magic.execute(code).asMap
        output.contains(MIMEType.PlainText) should be(true)
        output(MIMEType.PlainText).contains(DataFrameResponses.Usage) should be(true)

      }
    }
  }
}
