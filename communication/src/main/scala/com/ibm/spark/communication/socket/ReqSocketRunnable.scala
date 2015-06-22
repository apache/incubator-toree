package com.ibm.spark.communication.socket

import org.zeromq.ZMQ.{Socket, Context}

/**
 * Represents the runnable component of a socket that processes messages and
 * sends messages placed on an outbound queue. Targeted towards the request
 * socket, this runnable ensures that a message is sent out first and then a
 * response is received before sending the next message.
 *
 * @param context The ZMQ context to use with this runnable to create a socket
 * @param inboundMessageCallback The callback to invoke when receiving a message
 *                               on the socket created
 * @param socketOptions The options to use when creating the socket
 */
class ReqSocketRunnable(
  private val context: Context,
  private val inboundMessageCallback: Option[(Seq[String]) => Unit],
  private val socketOptions: SocketOption*
) extends ZeroMQSocketRunnable(
  context,
  ReqSocket,
  inboundMessageCallback,
  socketOptions: _*
) {
  /** Does nothing. */
  override protected def processNextInboundMessage(
    socket: Socket,
    flags: Int
  ): Unit = {}

  /**
   * Sends a message and then waits for an incoming response (if a message
   * was sent from the outbound queue).
   *
   * @param socket The socket to use when sending the message
   *
   * @return True if a message was sent, otherwise false
   */
  override protected def processNextOutboundMessage(socket: Socket): Boolean = {
    val shouldReceiveMessage = super.processNextOutboundMessage(socket)

    if (shouldReceiveMessage) {
      super.processNextInboundMessage(socket, 0)
    }

    shouldReceiveMessage
  }
}

