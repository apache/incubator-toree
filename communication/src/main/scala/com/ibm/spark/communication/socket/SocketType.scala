package com.ibm.spark.communication.socket

import org.zeromq.ZMQ

/**
 * Represents the type option used to indicate the type of socket to create.
 *
 * @param `type` The type as an integer
 */
sealed class SocketType(val `type`: Int)

case object PubSocket extends SocketType(ZMQ.PUB)
case object SubSocket extends SocketType(ZMQ.SUB)
case object RepSocket extends SocketType(ZMQ.REP)
case object ReqSocket extends SocketType(ZMQ.REQ)
case object RouterSocket extends SocketType(ZMQ.ROUTER)
case object DealerSocket extends SocketType(ZMQ.DEALER)
