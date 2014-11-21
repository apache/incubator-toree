package com.ibm.spark.magic.builtin

import com.ibm.spark.kernel.protocol.v5.MIMEType
import com.ibm.spark.magic._

class LSMagic extends MagicTemplate {
  /**
   * Lists all available magics
   * @param code The single line of code
   * @return The output of the magic
   */
  override def executeLine(code: String): MagicOutput = {
      val classes = new BuiltinLoader().getBuiltinClasses
      val magics = classes.map("%" + _.getSimpleName).mkString(" ")
      val result =
        s"Available magics:\n$magics\n\nType %<magic_name> for usage info."
      MagicOutput(MIMEType.PlainText -> result)
  }

  /**
   * Unsupported for this magic.
   * @param code The list of code, separated by newlines
   * @return The output of the magic
   */
  override def executeCell(code: Seq[String]): MagicOutput = {
    MagicOutput()
  }
}
