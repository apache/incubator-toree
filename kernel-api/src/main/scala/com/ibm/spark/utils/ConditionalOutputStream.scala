package com.ibm.spark.utils

import java.io.OutputStream

class ConditionalOutputStream(
  private val outputStream: OutputStream,
  condition: => Boolean
) extends OutputStream {
  require(outputStream != null)

  override def write(b: Int): Unit = if (condition) outputStream.write(b)
}
