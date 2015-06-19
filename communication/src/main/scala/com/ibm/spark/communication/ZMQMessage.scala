package com.ibm.spark.communication

import akka.util.ByteString

case class ZMQMessage(frames: ByteString*) {
  def frame(i: Int) = frames(i)
}
