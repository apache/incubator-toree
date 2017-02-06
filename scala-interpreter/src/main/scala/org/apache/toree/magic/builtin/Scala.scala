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
import org.apache.toree.kernel.interpreter.scala.{ScalaException, ScalaInterpreter}
import org.apache.toree.magic.dependencies.IncludeKernel
import org.apache.toree.magic.{CellMagic, MagicOutput}
import org.apache.toree.plugins.annotations.Event
/**
 * Represents the magic interface to use the Scala interpreter.
 */
class Scala extends CellMagic with IncludeKernel {

  @Event(name = "scala")
  override def execute(code: String): MagicOutput = {
    val scala = kernel.interpreter("Scala")

    if (scala.isEmpty || scala.get == null)
      throw new ScalaException("Scala is not available!")

    scala.get match {
      case scalaInterpreter: ScalaInterpreter =>
        val (_, output) = scalaInterpreter.interpret(code)
        output match {
          case Left(executeOutput) =>
            MagicOutput(executeOutput.toSeq:_*)
          case Right(executeFailure) => executeFailure match {
            case executeAborted: ExecuteAborted =>
              throw new ScalaException("Scala code was aborted!")
            case executeError: ExecuteError =>
              throw new ScalaException(executeError.value)
          }
        }
      case otherInterpreter =>
        val className = otherInterpreter.getClass.getName
        throw new ScalaException(s"Invalid Scala interpreter: $className")
    }
  }
}

