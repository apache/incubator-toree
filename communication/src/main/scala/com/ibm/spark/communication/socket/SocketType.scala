package com.ibm.spark.communication.socket

import org.zeromq.ZMQ

/**
 * Represents the type option used to indicate the type of socket to create.
 *
 * @param `type` The type as an integer
 */
sealed class SocketType(val `type`: Int)

/** Represents a publish socket. */
case object PubSocket extends SocketType(ZMQ.PUB)

/** Represents a subscribe socket. */
case object SubSocket extends SocketType(ZMQ.SUB)

/** Represents a reply socket. */
case object RepSocket extends SocketType(ZMQ.REP)

/** Represents a request socket. */
case object ReqSocket extends SocketType(ZMQ.REQ)

/** Represents a router socket. */
case object RouterSocket extends SocketType(ZMQ.ROUTER)

/** Represents a dealer socket. */
case object DealerSocket extends SocketType(ZMQ.DEALER)
