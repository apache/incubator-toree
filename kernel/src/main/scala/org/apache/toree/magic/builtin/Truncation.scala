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

import org.apache.toree.magic.LineMagic
import org.apache.toree.magic.dependencies.IncludeOutputStream
import java.io.PrintStream
import org.apache.toree.kernel.api.KernelOptions
import org.apache.toree.plugins.annotations.Event


class Truncation extends LineMagic with IncludeOutputStream {
  private def printStream = new PrintStream(outputStream)

  @Event(name = "truncation")
  override def execute(code: String): Unit = {
    code match {
      case "on" =>
        printStream.println(s"Output WILL be truncated.")
        KernelOptions.noTruncation = false
      case "off" =>
        printStream.println(s"Output will NOT be truncated")
        KernelOptions.noTruncation = true
      case "" =>
        printStream.println(s"Truncation is currently ${if (KernelOptions.noTruncation) "off" else "on"} ")
      case other =>
        printStream.println(s"${other} is not a valid option for the NoTruncation magic.")
    }
  }
}
