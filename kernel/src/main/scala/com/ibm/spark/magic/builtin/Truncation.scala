package com.ibm.spark.magic.builtin

import com.ibm.spark.magic.LineMagic
import com.ibm.spark.magic.dependencies.IncludeOutputStream
import java.io.PrintStream
import com.ibm.spark.kernel.api.KernelOptions


class Truncation extends LineMagic with IncludeOutputStream {
  private lazy val printStream = new PrintStream(outputStream)

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
