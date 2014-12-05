package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.Actor
import akka.util.ByteString
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.Utilities._
import com.ibm.spark.utils.{MessageLogSupport, LogLike}

/**
 * The server endpoint for IOPub messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class IOPub(socketFactory: ServerSocketFactory)
  extends Actor with MessageLogSupport {
  logger.trace("Created new IOPub actor")
  val socket = socketFactory.IOPub(context.system)
  override def receive: Receive = {
    case message: KernelMessage =>
      val zmqMessage: ZMQMessage = message
      logMessage(message)
      socket ! zmqMessage
  }
}
