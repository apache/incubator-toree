package com.ibm.spark.client.exception

class ShellException(e: Throwable) extends Throwable {
  val exception: Throwable = e
}
