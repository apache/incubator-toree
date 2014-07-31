package com.ibm.spark.utils

import java.io.Writer

case class MultiWriter(val outputWriters: List[Writer])
  extends Writer
{
  require(outputWriters != null)

  override def append(c: Char): Writer = {
    outputWriters.foreach(writer => writer.append(c))
    this
  }

  override def append(csq: CharSequence): Writer = {
    outputWriters.foreach(writer => writer.append(csq))
    this
  }

  override def append(csq: CharSequence, start: Int, end: Int): Writer = {
    outputWriters.foreach(writer => writer.append(csq, start, end))
    this
  }

  override def write(cbuf: Array[Char]): Unit =
    outputWriters.foreach(writer => writer.write(cbuf))

  override def write(cbuf: Array[Char], off: Int, len: Int): Unit =
    outputWriters.foreach(writer => writer.write(cbuf, off, len))

  override def write(b: Int): Unit =
    outputWriters.foreach(writer => writer.write(b))

  override def write(str: String): Unit =
    outputWriters.foreach(writer => writer.write(str))

  override def write(str: String, off: Int, len: Int): Unit =
    outputWriters.foreach(writer => writer.write(str, off, len))

  override def flush() =
    outputWriters.foreach(writer => writer.flush())

  override def close() =
    outputWriters.foreach(writer => writer.close())
}
