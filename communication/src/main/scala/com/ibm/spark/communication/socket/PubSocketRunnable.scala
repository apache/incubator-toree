package com.ibm.spark.communication.socket

import org.zeromq.ZMQ.{Socket, Context}

/**
 * Represents the runnable component of a socket specifically targeted towards
 * publish sockets. No incoming messages are processed.
 *
 * @param context The ZMQ context to use with this runnable to create a socket
 * @param socketOptions The options to use when creating the socket
 */
class PubSocketRunnable(
  private val context: Context,
  private val socketOptions: SocketOption*
) extends ZeroMQSocketRunnable(
  context,
  PubSocket,
  None,
  socketOptions: _*
) {
  /** Does nothing. */
  override protected def processNextInboundMessage(
    socket: Socket,
    flags: Int
  ): Unit = {}
}
