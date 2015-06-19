package com.ibm.spark.communication.socket

import org.zeromq.ZMQ

/** Represents an option to provide to a socket. */
sealed trait SocketOption

/**
 * Represents the linger option used to communicate the millisecond duration
 * to continue processing messages after the socket has been told to close.
 *
 * @note Provide -1 as the duration to wait until all messages are processed
 *
 * @param milliseconds The duration in milliseconds
 */
case class Linger(milliseconds: Int) extends SocketOption

/**
 * Represents the subscribe option used to filter messages coming into a
 * socket subscribing to a publisher. Uses the provided byte prefix to filter
 * incoming messages.
 *
 * @param topic The array of bytes to use as a filter based on the
 *              bytes at the beginning of incoming messages
 */
case class Subscribe(topic: Array[Byte]) extends SocketOption
object Subscribe {
  val all = Subscribe(ZMQ.SUBSCRIPTION_ALL)
}

/**
 * Represents the identity option used to identify the socket.
 *
 * @param identity The identity to use with the socket
 */
case class Identity(identity: Array[Byte]) extends SocketOption

/**
 * Represents the bind option used to tell the socket what address to bind to.
 *
 * @param address The address for the socket to use
 */
case class Bind(address: String) extends SocketOption

/**
 * Represents the connect option used to tell the socket what address to
 * connect to.
 *
 * @param address The address for the socket to use
 */
case class Connect(address: String) extends SocketOption
