package com.ibm.spark.communication.socket

/**
 * Represents a generic interface for socket communication.
 */
trait SocketLike {
  def send(message: String*): Unit
  def close(): Unit
}

