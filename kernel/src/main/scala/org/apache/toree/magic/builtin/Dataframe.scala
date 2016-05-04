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

import java.io.{PrintStream, StringWriter}

import org.apache.toree.interpreter.{ExecuteAborted, ExecuteError, ExecuteFailure, Results}
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.magic._
import org.apache.toree.magic.dependencies.{IncludeKernelInterpreter, IncludeOutputStream}
import org.apache.toree.plugins.annotations.{Event, Init}
import org.apache.toree.utils.{ArgumentParsingSupport, DataFrameConverter, LogLike}

import scala.util.Try


class DFConversionException extends Exception{}


object DataFrameResponses {
  val MagicAborted = s"${classOf[DataFrame].getSimpleName} magic aborted!"

  def ErrorMessage(outputType: String, error: String) = {
    s"An error occurred converting DataFrame to ${outputType}.\n${error}"
  }

  def NoVariableFound(name: String) = {
    s"No variable found with the name ${name}!"
  }

  val Incomplete = "DataFrame code was an incomplete code snippet"

  val Usage  =
    """%%dataframe [arguments]
      |DATAFRAME_CODE
      |
      |DATAFRAME_CODE can be any numbered lines of code, as long as the
      |last line is a reference to a variable which is a DataFrame.
    """.stripMargin
}

class DataFrame extends CellMagic with IncludeKernelInterpreter
  with IncludeOutputStream with ArgumentParsingSupport with LogLike {
  private var _dataFrameConverter: DataFrameConverter = _
  private val outputTypeMap = Map[String, String](
    "html" -> MIMEType.TextHtml,
    "csv" -> MIMEType.PlainText,
    "json" -> MIMEType.ApplicationJson
  )

  @Init def initMethod(dataFrameConverter: DataFrameConverter) = {
    _dataFrameConverter = dataFrameConverter
  }
  private def printStream = new PrintStream(outputStream)
  
  private val _outputType = parser.accepts(
    "output", "The type of the output: html, csv, json"
  ).withRequiredArg().defaultsTo("html")

  private val _limit = parser.accepts(
    "limit", "The number of records to return"
  ).withRequiredArg().defaultsTo("10")

  private def outputType(): String = {
    _outputType.getOrElse("html")
  }
  private def limit(): Int = {
    _limit.getOrElse("10").toInt
  }
  
  private def outputTypeToMimeType(): String = {
    outputTypeMap.getOrElse(outputType, MIMEType.PlainText)
  }
  
  private def convertToJson(rddCode: String): CellMagicOutput = {
    val (result, message) = kernelInterpreter.interpret(rddCode)
    result match {
      case Results.Success =>
        val rddVarName = kernelInterpreter.lastExecutionVariableName.get
        kernelInterpreter.read(rddVarName).map(variableVal => {
          _dataFrameConverter.convert(
            variableVal.asInstanceOf[org.apache.spark.sql.DataFrame],
            outputType,
            limit
          ).map(output =>
            CellMagicOutput(outputTypeToMimeType -> output)
          ).get
        }).getOrElse(CellMagicOutput(MIMEType.PlainText -> DataFrameResponses.NoVariableFound(rddVarName)))
      case Results.Aborted =>
        logger.error(DataFrameResponses.ErrorMessage(outputType, DataFrameResponses.MagicAborted))
        CellMagicOutput(
          MIMEType.PlainText -> DataFrameResponses.ErrorMessage(outputType, DataFrameResponses.MagicAborted)
        )
      case Results.Error =>
        val error = message.right.get.asInstanceOf[ExecuteError]
        val errorMessage = DataFrameResponses.ErrorMessage(outputType, error.value)
        logger.error(errorMessage)
        CellMagicOutput(MIMEType.PlainText -> errorMessage)
      case Results.Incomplete =>
        logger.error(DataFrameResponses.Incomplete)
        CellMagicOutput(MIMEType.PlainText -> DataFrameResponses.Incomplete)
    }
  }

  private def helpToCellMagicOutput(optionalException: Option[Exception] = None): CellMagicOutput = {
    val stringWriter = new StringWriter()
    stringWriter.append(optionalException.map(e => {
      s"ERROR: ${e.getMessage}\n"
    }).getOrElse(""))
    stringWriter.write(DataFrameResponses.Usage)
    parser.printHelpOn(stringWriter)
    CellMagicOutput(MIMEType.PlainText -> stringWriter.toString)
  }
  
  @Event(name = "dataframe")
  override def execute(code: String): CellMagicOutput = {
    val lines = code.trim.split("\n")
    Try({
      val res: CellMagicOutput = if (lines.length == 1 && lines.head.length == 0){
        helpToCellMagicOutput()
      } else if (lines.length == 1) {
        parseArgs("")
        convertToJson(lines.head)
      } else {
        parseArgs(lines.head)
        convertToJson(lines.drop(1).reduce(_ + _))
      }
      res
    }).recover({
      case e: Exception =>
        helpToCellMagicOutput(Some(e))
    }).get
  }
}