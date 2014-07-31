package com.ibm.spark.utils

import java.io.OutputStream

case class MultiOutputStream(val outputStreams: List[OutputStream])
  extends OutputStream
{
  require(outputStreams != null)

  override def write(b: Int): Unit =
    outputStreams.foreach(outputStream => outputStream.write(b))

  override def write(b: Array[Byte]) =
    outputStreams.foreach(outputStream => outputStream.write(b))

  override def write(b: Array[Byte], off: Int, len: Int) =
    outputStreams.foreach(outputStream => outputStream.write(b, off, len))

  override def flush() =
    outputStreams.foreach(outputStream => outputStream.flush())

  override def close() =
    outputStreams.foreach(outputStream => outputStream.close())
}
