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

package com.ibm.spark.magic.builtin

import java.io.PrintStream

import com.ibm.spark.kernel.protocol.v5.MIMEType
import com.ibm.spark.magic._
import com.ibm.spark.magic.dependencies.IncludeOutputStream
import com.ibm.spark.utils.ArgumentParsingSupport
import com.google.common.base.Strings

class Html extends MagicTemplate with ArgumentParsingSupport
  with IncludeOutputStream {

  // Lazy because the outputStream is not provided at construction
  private lazy val printStream = new PrintStream(outputStream)

  override def executeCell(code: Seq[String]): MagicOutput = {
    MagicOutput(MIMEType.TextHtml -> code.mkString("\n"))
  }

  override def executeLine(code: String): MagicOutput = {
    def printHelpAndReturn: MagicOutput = {
      printHelp(printStream, """%Html <string_code>""")
      MagicOutput()
    }

    Strings.isNullOrEmpty(code) match {
      case true => printHelpAndReturn
      case false => MagicOutput(MIMEType.TextHtml -> code)
    }
  }
}
