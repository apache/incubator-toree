package com.ibm.spark.magic.builtin

import com.ibm.spark.kernel.protocol.v5.MIMEType
import com.ibm.spark.magic.MagicOutput
import com.ibm.spark.magic.dependencies.IncludeInterpreter

class html extends MagicTemplate with IncludeInterpreter {
  /**
   * Execute a magic representing a line magic.
   * @param code The single line of code
   * @return The output of the magic
   */
  override def executeLine(code: String): MagicOutput =
    MagicOutput(MIMEType.TextHtml -> code, MIMEType.PlainText -> "html")

  /**
   * Execute a magic representing a cell magic.
   * @param code The list of code, separated by newlines
   * @return The output of the magic
   */
  override def executeCell(code: Seq[String]): MagicOutput = {
    val str = code.mkString("\n")
    val codeTuple = interpreter.interpret(s"println($str)")

    MagicOutput(MIMEType.TextHtml -> codeTuple._2.left.get)
  }
}
