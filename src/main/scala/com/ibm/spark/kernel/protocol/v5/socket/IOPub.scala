package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.{Actor, ActorLogging}

/**
 * The server endpoint for IOPub messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class IOPub(socketFactory: SocketFactory) extends Actor with ActorLogging {
  log.debug("Created new IOPub actor")
  val socket = socketFactory.IOPub(context.system)
  override def receive: Receive = {
    case message =>
      log.debug("Sending IOPub message")
      println("IOPUB SENDING: " + message)
      socket ! message
  }
}
