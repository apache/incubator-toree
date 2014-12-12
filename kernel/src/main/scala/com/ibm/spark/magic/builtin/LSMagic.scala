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

import com.ibm.spark.kernel.protocol.v5.MIMEType
import com.ibm.spark.magic._

class LSMagic extends MagicTemplate {

  val cellUnsupportedMessage =
    "Cell magic unsupported. Please use the line magic %lsmagic."

  /**
   * Lists all available magics
   * @param code The single line of code
   * @return The output of the magic
   */
  override def executeLine(code: String): MagicOutput = {
      val classes = new BuiltinLoader().getClasses()
      val magics = classes.map("%" + _.getSimpleName).mkString(" ").toLowerCase
      val message =
        s"Available magics:\n$magics\n\nType %<magic_name> for usage info."
      MagicOutput(MIMEType.PlainText -> message)
  }

  /**
   * Unsupported for this magic.
   * @param code The list of code, separated by newlines
   * @return The output of the magic
   */
  override def executeCell(code: Seq[String]): MagicOutput = {
    MagicOutput(MIMEType.PlainText -> cellUnsupportedMessage)
  }
}
