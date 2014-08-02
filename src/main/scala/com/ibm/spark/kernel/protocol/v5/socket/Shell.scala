package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.{Actor, ActorLogging}

/**
 * The server endpoint for shell messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class Shell(socketFactory: SocketFactory) extends Actor with ActorLogging {
  log.debug("Created new Shell actor")
  val socket = socketFactory.Shell(context.system, self)
  override def receive: Receive = {
    case message =>
      log.debug("Sending shell message")
      println("SHELL SENDING: " + message)
      socket ! message
  }
}
