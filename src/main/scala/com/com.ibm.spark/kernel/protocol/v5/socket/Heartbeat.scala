package com.ibm.kernel.protocol.v5.socket

import akka.actor.{ActorLogging, Actor}

/**
 * The server endpoint for heartbeat messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class Heartbeat(socketFactory : SocketFactory) extends Actor with ActorLogging {
  log.debug("Created new Heartbeat actor")
  val socket = socketFactory.Heartbeat(context.system, self)
  override def receive: Receive = {
    case message =>
      log.debug("Sending heartbeat message")
      socket ! message
  }
}
