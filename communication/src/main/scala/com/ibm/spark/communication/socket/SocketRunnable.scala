package com.ibm.spark.communication.socket

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Represents the interface for a runnable used to send and receive messages
 * for a socket.
 *
 * @param inboundMessageCallback The callback to use when receiving a message
 *                               through this runnable
 */
abstract class SocketRunnable[T](
   private val inboundMessageCallback: Option[(Seq[String]) => Unit]
) extends Runnable {

  /** The collection of messages to be sent out through the socket. */
  val outboundMessages: ConcurrentLinkedQueue[T] =
    new ConcurrentLinkedQueue[T]()

  /**
   * Attempts to add a new message to the outbound queue to be sent out.
   *
   * @param message The message to add to the queue
   *
   * @return True if successfully queued the message, otherwise false
   */
  def offer(message: T): Boolean = outboundMessages.offer(message)

  /**
   * Indicates whether or not the runnable is processing messages (both
   * sending and receiving).
   *
   * @return True if processing, otherwise false
   */
  def isProcessing: Boolean

  /**
   * Closes the runnable such that it no longer processes messages and also
   * closes the underlying socket associated with the runnable.
   */
  def close(): Unit
}
