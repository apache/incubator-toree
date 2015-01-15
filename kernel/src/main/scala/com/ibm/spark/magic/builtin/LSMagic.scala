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

import com.ibm.spark.magic._
import com.ibm.spark.magic.dependencies.IncludeOutputStream

class LSMagic extends LineMagic with IncludeOutputStream {

  private lazy val printStream = new PrintStream(outputStream)

  /**
   * Lists all available magics
   * @param code The single line of code
   * @return The output of the magic
   */
  override def execute(code: String): Unit = {
    val classes = new BuiltinLoader().getClasses()
    val magics = classes.map("%" + _.getSimpleName).mkString(" ").toLowerCase
    val message =
      s"Available magics:\n$magics\n\nType %<magic_name> for usage info."
    printStream.println(message)
  }
}
