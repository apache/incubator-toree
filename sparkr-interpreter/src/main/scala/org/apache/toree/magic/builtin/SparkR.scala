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
package org.apache.toree.magic.builtin

import org.apache.toree.interpreter.{ExecuteError, ExecuteAborted}
import org.apache.toree.kernel.interpreter.sparkr.{SparkRInterpreter, SparkRException}
import org.apache.toree.kernel.protocol.v5.MIMEType
import org.apache.toree.magic.{CellMagicOutput, CellMagic}
import org.apache.toree.magic.dependencies.IncludeKernel

/**
 * Represents the magic interface to use the SparkR interpreter.
 */
class SparkR extends CellMagic with IncludeKernel {
  override def execute(code: String): CellMagicOutput = {
    val sparkR = kernel.interpreter("SparkR")

    if (sparkR.isEmpty || sparkR.get == null)
      throw new SparkRException("SparkR is not available!")

    sparkR.get match {
      case sparkRInterpreter: SparkRInterpreter =>
        val (_, output) = sparkRInterpreter.interpret(code)
        output match {
          case Left(executeOutput) =>
            CellMagicOutput(MIMEType.PlainText -> executeOutput)
          case Right(executeFailure) => executeFailure match {
            case executeAborted: ExecuteAborted =>
              throw new SparkRException("SparkR code was aborted!")
            case executeError: ExecuteError =>
              throw new SparkRException(executeError.value)
          }
        }
      case otherInterpreter =>
        val className = otherInterpreter.getClass.getName
        throw new SparkRException(s"Invalid SparkR interpreter: $className")
    }
  }
}
