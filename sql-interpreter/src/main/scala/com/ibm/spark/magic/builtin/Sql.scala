/*
 * Copyright 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.spark.magic.builtin

import com.ibm.spark.interpreter.{ExecuteError, ExecuteAborted}
import com.ibm.spark.kernel.interpreter.sql.{SqlInterpreter, SqlException}
import com.ibm.spark.kernel.protocol.v5.MIMEType
import com.ibm.spark.magic.{CellMagicOutput, CellMagic}
import com.ibm.spark.magic.dependencies.IncludeKernel

/**
 * Represents the magic interface to use the SQL interpreter.
 */
class Sql extends CellMagic with IncludeKernel {
  override def execute(code: String): CellMagicOutput = {
    val sparkR = Option(kernel.data.get("SQL"))

    if (sparkR.isEmpty || sparkR.get == null)
      throw new SqlException("SQL is not available!")

    sparkR.get match {
      case sparkRInterpreter: SqlInterpreter =>
        val (_, output) = sparkRInterpreter.interpret(code)
        output match {
          case Left(executeOutput) =>
            CellMagicOutput(MIMEType.PlainText -> executeOutput)
          case Right(executeFailure) => executeFailure match {
            case executeAborted: ExecuteAborted =>
              throw new SqlException("SQL code was aborted!")
            case executeError: ExecuteError =>
              throw new SqlException(executeError.value)
          }
        }
      case otherInterpreter =>
        val className = otherInterpreter.getClass.getName
        throw new SqlException(s"Invalid SQL interpreter: $className")
    }
  }
}

