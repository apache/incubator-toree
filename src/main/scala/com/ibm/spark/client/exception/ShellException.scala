package com.ibm.spark.client.exception

/**
 * Created by Chris on 8/11/14.
 */
class ShellException(e: Throwable) extends Throwable {
  val exception: Throwable = e
}
