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

import org.apache.toree.interpreter.{ExecuteFailure, Results, ExecuteAborted, ExecuteError}
import org.apache.toree.kernel.protocol.v5.MIMEType
import org.apache.toree.magic._
import org.apache.toree.magic.dependencies.{IncludeKernelInterpreter, IncludeInterpreter}
import org.apache.toree.utils.LogLike
import org.apache.toree.utils.json.RddToJson
import org.apache.spark.sql.SchemaRDD

/**
 * Temporary magic to show an RDD as JSON
 */
class RDD extends CellMagic with IncludeKernelInterpreter with LogLike {

  private def convertToJson(code: String) = {
    val (result, message) = kernelInterpreter.interpret(code)
    result match {
      case Results.Success =>
        val rddVarName = kernelInterpreter.lastExecutionVariableName.getOrElse("")
        kernelInterpreter.read(rddVarName).map(rddVal => {
          try{
            CellMagicOutput(MIMEType.ApplicationJson -> RddToJson.convert(rddVal.asInstanceOf[SchemaRDD]))
          } catch {
            case _: Throwable =>
              CellMagicOutput(MIMEType.PlainText -> s"Could note convert RDD to JSON: ${rddVarName}->${rddVal}")
          }
        }).getOrElse(CellMagicOutput(MIMEType.PlainText -> "No RDD Value found!"))
      case _ =>
        val errorMessage = message.right.toOption match {
          case Some(executeFailure) => executeFailure match {
            case _: ExecuteAborted => throw new Exception("RDD magic aborted!")
            case executeError: ExecuteError => throw new Exception(executeError.value)
          }
          case _ =>  "No error information available!"
        }
        logger.error(s"Error retrieving RDD value: ${errorMessage}")
        CellMagicOutput(MIMEType.PlainText ->
          (s"An error occurred converting RDD to JSON.\n${errorMessage}"))
    }
  }

  override def execute(code: String): CellMagicOutput =
    convertToJson(code)
}