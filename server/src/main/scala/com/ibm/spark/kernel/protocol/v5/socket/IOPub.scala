package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.Actor
import akka.util.ByteString
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.utils.LogLike
import com.ibm.spark.kernel.protocol.v5.Utilities._

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
      logger.info("IOPUB SENDING: " +
        zmqMessage.frames.map((byteString: ByteString) => new String(byteString.toArray)).mkString("\n"))
      //val bytes: ByteString = zmqMessage.frames.foldLeft[ByteString](ByteString("")){(s1, s2) => s1 ++ s2}
      socket ! zmqMessage
  }
}
