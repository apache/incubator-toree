package com.ibm.spark.utils

import java.io.OutputStream

case class MultiOutputStream(val outputOutputStreams: List[OutputStream])
  extends OutputStream
{
  require(outputOutputStreams != null)

  override def write(cbuf: Array[Byte]): Unit =
    outputOutputStreams.foreach(outputStream => outputStream.write(cbuf))

  override def write(cbuf: Array[Byte], off: Int, len: Int): Unit =
    outputOutputStreams.foreach(outputStream => outputStream.write(cbuf, off, len))

  override def write(b: Int): Unit =
    outputOutputStreams.foreach(outputStream => outputStream.write(b))

  override def flush() =
    outputOutputStreams.foreach(outputStream => outputStream.flush())

  override def close() =
    outputOutputStreams.foreach(outputStream => outputStream.close())
}
