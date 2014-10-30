package com.ibm.spark.utils

import java.io.OutputStream

case class MultiOutputStream(val outputStreams: List[OutputStream])
  extends OutputStream
{
  require(outputStreams != null)

  override def write(cbuf: Array[Byte]): Unit =
    outputStreams.foreach(outputStream => outputStream.write(cbuf))

  override def write(cbuf: Array[Byte], off: Int, len: Int): Unit =
    outputStreams.foreach(outputStream => outputStream.write(cbuf, off, len))

  override def write(b: Int): Unit =
    outputStreams.foreach(outputStream => outputStream.write(b))

  override def flush() =
    outputStreams.foreach(outputStream => outputStream.flush())

  override def close() =
    outputStreams.foreach(outputStream => outputStream.close())
}
