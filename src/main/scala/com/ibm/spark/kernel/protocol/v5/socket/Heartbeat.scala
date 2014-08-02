package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.{ActorLogging, Actor}
import akka.util.ByteString
import akka.zeromq.ZMQMessage

/**
 * The server endpoint for heartbeat messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class Heartbeat(socketFactory : SocketFactory) extends Actor with ActorLogging {
  log.debug("Created new Heartbeat actor")
  val socket = socketFactory.Heartbeat(context.system, self)

  override def receive: Receive = {
    case message: ZMQMessage =>
      log.debug("Sending heartbeat message")
      println("HEARTBEAT RECEIVING: " +
        message.frames.map((byteString: ByteString) => new String(byteString.toArray)).mkString("\n"))
      println("HEARTBEAT SENDING: " +
        message.frames.map((byteString: ByteString) => new String(byteString.toArray)).mkString("\n"))
      socket ! message
  }
}
