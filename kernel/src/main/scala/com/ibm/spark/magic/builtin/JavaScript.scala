package com.ibm.spark.magic.builtin

import java.io.PrintStream

import com.google.common.base.Strings
import com.ibm.spark.kernel.protocol.v5.MIMEType
import com.ibm.spark.magic._
import com.ibm.spark.magic.dependencies.IncludeOutputStream
import com.ibm.spark.utils.ArgumentParsingSupport
import org.slf4j.LoggerFactory

class JavaScript extends MagicTemplate with ArgumentParsingSupport
  with IncludeOutputStream {

  // Lazy because the outputStream is not provided at construction
  private lazy val printStream = new PrintStream(outputStream)

  override def executeCell(code: Seq[String]): MagicOutput = {
    MagicOutput(MIMEType.ApplicationJavaScript -> code.mkString("\n"))
  }

  override def executeLine(code: String): MagicOutput = {
    def printHelpAndReturn: MagicOutput = {
      printHelp(printStream, """%JavaScript <string_code>""")
      MagicOutput()
    }

    Strings.isNullOrEmpty(code) match {
      case true => printHelpAndReturn
      case false => MagicOutput(MIMEType.ApplicationJavaScript -> code)
    }
  }
}
