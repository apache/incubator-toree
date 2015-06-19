package com.ibm.spark.communication.socket

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Represents the interface for a runnable used to send and receive messages
 * for a socket.
 */
abstract class SocketRunnable[T](
   private val inboundMessageCallback: Option[(Seq[String]) => Unit]
) extends Runnable {

  val outboundMessages: ConcurrentLinkedQueue[T] =
    new ConcurrentLinkedQueue[T]()

  def offer(message: T): Boolean = outboundMessages.offer(message)

  def close(): Unit
}
