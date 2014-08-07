package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.Actor
import akka.util.ByteString
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5.KernelMessage
import com.ibm.spark.utils.LogLike

/**
 * The server endpoint for IOPub messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class IOPub(socketFactory: SocketFactory) extends Actor with LogLike {
  logger.debug("Created new IOPub actor")
  val socket = socketFactory.IOPub(context.system)
  override def receive: Receive = {
    case message: KernelMessage =>
      val zmqMessage: ZMQMessage = message
      println("IOPUB SENDING: " +
        zmqMessage.frames.map((byteString: ByteString) => new String(byteString.toArray)).mkString("\n"))
      socket ! zmqMessage
  }
}
