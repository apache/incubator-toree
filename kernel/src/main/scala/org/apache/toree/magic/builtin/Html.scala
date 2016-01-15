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

import java.io.PrintStream

import org.apache.toree.kernel.protocol.v5.MIMEType
import org.apache.toree.magic._
import org.apache.toree.magic.dependencies.IncludeOutputStream
import org.apache.toree.utils.ArgumentParsingSupport
import com.google.common.base.Strings

class Html extends CellMagic with ArgumentParsingSupport
  with IncludeOutputStream {

  // Lazy because the outputStream is not provided at construction
  private lazy val printStream = new PrintStream(outputStream)

  override def execute(code: String): CellMagicOutput = {
    def printHelpAndReturn: CellMagicOutput = {
      printHelp(printStream, """%%Html <string_code>""")
      CellMagicOutput()
    }

    Strings.isNullOrEmpty(code) match {
      case true => printHelpAndReturn
      case false => CellMagicOutput(MIMEType.TextHtml -> code)
    }
  }
}
