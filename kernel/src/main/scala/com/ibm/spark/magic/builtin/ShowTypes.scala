package com.ibm.spark.magic.builtin

import com.ibm.spark.magic.LineMagic
import com.ibm.spark.magic.dependencies.IncludeOutputStream
import java.io.PrintStream
import com.ibm.spark.kernel.api.KernelOptions


class ShowTypes extends LineMagic with IncludeOutputStream {
  private lazy val printStream = new PrintStream(outputStream)

  override def execute(code: String): Unit = {
    code match {
      case "on" =>
        printStream.println(s"Types will be printed.")
        KernelOptions.showTypes = true
      case "off" =>
        printStream.println(s"Types will not be printed")
        KernelOptions.showTypes = false
      case "" =>
        printStream.println(s"ShowTypes is currently ${if (KernelOptions.showTypes) "on" else "off"} ")
      case other =>
        printStream.println(s"${other} is not a valid option for the ShowTypes magic.")
    }
  }
}
