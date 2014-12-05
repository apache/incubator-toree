package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.Actor
import akka.util.ByteString
import akka.zeromq.ZMQMessage
import com.ibm.spark.utils.LogLike

/**
 * The server endpoint for heartbeat messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class Heartbeat(socketFactory : ServerSocketFactory) extends Actor with LogLike {
  logger.debug("Created new Heartbeat actor")
  val socket = socketFactory.Heartbeat(context.system, self)

  override def receive: Receive = {
    case message: ZMQMessage =>
      logger.trace("Heartbeat received message: " +
        message.frames.map((byteString: ByteString) =>
          new String(byteString.toArray)).mkString("\n"))
      socket ! message
  }
}