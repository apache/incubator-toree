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

import org.apache.toree.interpreter.{ExecuteError, ExecuteAborted}
import org.apache.toree.kernel.interpreter.sql.{SqlInterpreter, SqlException}
import org.apache.toree.magic.{MagicOutput, CellMagic}
import org.apache.toree.magic.dependencies.IncludeKernel
import org.apache.toree.plugins.annotations.Event

/**
 * Represents the magic interface to use the SQL interpreter.
 */
class Sql extends CellMagic with IncludeKernel {

  @Event(name = "sql")
  override def execute(code: String): MagicOutput = {
    val sparkR = kernel.interpreter("SQL")

    if (sparkR.isEmpty || sparkR.get == null)
      throw new SqlException("SQL is not available!")

    sparkR.get match {
      case sparkRInterpreter: SqlInterpreter =>
        val (_, output) = sparkRInterpreter.interpret(code)
        output match {
          case Left(executeOutput) =>
            MagicOutput(executeOutput.toSeq:_*)
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

