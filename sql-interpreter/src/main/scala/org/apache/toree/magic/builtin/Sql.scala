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

import org.apache.toree.interpreter.{ExecuteAborted, ExecuteError}
import org.apache.toree.kernel.interpreter.scala.ScalaInterpreter
import org.apache.toree.kernel.interpreter.sql.{SqlException, SqlInterpreter}
import org.apache.toree.magic.{CellMagic, MagicOutput}
import org.apache.toree.magic.dependencies.IncludeKernel
import org.apache.toree.plugins.annotations.Event

import scala.tools.nsc.interpreter
import scala.tools.nsc.interpreter.Results

/**
 * Represents the magic interface to use the SQL interpreter.
 */
class Sql extends CellMagic with IncludeKernel {

  @Event(name = "sql")
  override def execute(code: String): MagicOutput = {
    val sparkSql = kernel.interpreter("SQL")

    if (sparkSql.isEmpty || sparkSql.get == null)
      throw new SqlException("SQL is not available!")

    val scala = kernel.interpreter("Scala")
    val evaluated = if (scala.nonEmpty && scala.get != null) {
      val scalaInterpreter = scala.get.asInstanceOf[ScalaInterpreter]

      scalaInterpreter.iMain.interpret("s\"" + code.replace("\n", " ") + "\"") match {
        case Results.Success =>
          scalaInterpreter.iMain.valueOfTerm(scalaInterpreter.iMain.mostRecentVar).get.asInstanceOf[String]
        case _ => code
      }
    } else {
      code
    }

    sparkSql.get match {
      case sqlInterpreter: SqlInterpreter =>
        val (_, output) = sqlInterpreter.interpret(evaluated)
        output match {
          case Left(executeOutput) =>
            MagicOutput(executeOutput.toSeq: _*)
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

