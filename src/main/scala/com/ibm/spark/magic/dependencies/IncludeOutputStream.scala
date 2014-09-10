package com.ibm.spark.magic.dependencies

import java.io.OutputStream

import com.ibm.spark.magic.builtin.MagicTemplate

trait IncludeOutputStream {
  this: MagicTemplate =>

  //val outputStream: OutputStream
  private var _outputStream: OutputStream = _
  def outputStream: OutputStream = _outputStream
  def outputStream_=(newOutputStream: OutputStream) =
    _outputStream = newOutputStream
}
