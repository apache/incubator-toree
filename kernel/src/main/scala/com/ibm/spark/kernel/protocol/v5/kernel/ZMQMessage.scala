package com.ibm.spark.kernel.protocol.v5.kernel

import akka.util.ByteString

case class ZMQMessage(frames: ByteString*) {
  def frame(i: Int) = frames(i)
}
