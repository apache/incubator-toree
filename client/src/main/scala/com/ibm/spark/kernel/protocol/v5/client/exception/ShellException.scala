package com.ibm.spark.kernel.protocol.v5.client.exception

class ShellException(e: Throwable) extends Throwable {
  val exception: Throwable = e
}
