package com.ibm.spark.communication.socket

/**
 * Represents a generic interface for socket communication.
 */
trait SocketLike {
  /**
   * Sends a message through the socket if alive.
   *
   * @throws AssertionError If the socket is not alive when attempting to send
   *                        a message
   *
   * @param message The message to send
   */
  def send(message: String*): Unit

  /**
   * Closes the socket, marking it no longer able to process or send messages.
   */
  def close(): Unit

  /**
   * Returns whether or not the socket is alive (processing new messages and
   * capable of sending out messages).
   *
   * @return True if alive, otherwise false
   */
  def isAlive: Boolean

  /**
   * Returns whether or not the socket is ready to send/receive messages.
   *
   * @return True if ready, otherwise false
   */
  def isReady: Boolean
}

