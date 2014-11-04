package com.ibm.spark.magic.builtin

import com.ibm.spark.kernel.protocol.v5.MIMEType
import com.ibm.spark.magic.{MagicOutput, MagicTemplate}

class JavaScript extends MagicTemplate {
  override def executeCell(code: Seq[String]): MagicOutput = {
    MagicOutput(MIMEType.ApplicationJavaScript -> code.mkString("\n"))
  }

  override def executeLine(code: String): MagicOutput = {
    MagicOutput(MIMEType.ApplicationJavaScript -> code)
  }
}
