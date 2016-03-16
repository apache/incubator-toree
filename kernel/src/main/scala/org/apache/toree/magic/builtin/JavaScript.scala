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

import java.io.PrintStream

import com.google.common.base.Strings
import org.apache.toree.kernel.protocol.v5.MIMEType
import org.apache.toree.magic._
import org.apache.toree.magic.dependencies.IncludeOutputStream
import org.apache.toree.utils.ArgumentParsingSupport
import org.slf4j.LoggerFactory
import org.apache.toree.plugins.annotations.Event

class JavaScript extends CellMagic with ArgumentParsingSupport
  with IncludeOutputStream {

  // Lazy because the outputStream is not provided at construction
  private def printStream = new PrintStream(outputStream)
  
  @Event(name = "javascript")
  override def execute(code: String): CellMagicOutput = {
    def printHelpAndReturn: CellMagicOutput = {
      printHelp(printStream, """%JavaScript <string_code>""")
      CellMagicOutput()
    }

    Strings.isNullOrEmpty(code) match {
      case true => printHelpAndReturn
      case false => CellMagicOutput(MIMEType.ApplicationJavaScript -> code)
    }
  }
}
