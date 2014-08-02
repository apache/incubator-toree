package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.{Actor, ActorLogging}
import akka.util.ByteString
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5.KernelMessage

/**
 * The server endpoint for IOPub messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class IOPub(socketFactory: SocketFactory) extends Actor with ActorLogging {
  log.debug("Created new IOPub actor")
  val socket = socketFactory.IOPub(context.system)
  override def receive: Receive = {
    case message: KernelMessage =>
      val zmqMessage: ZMQMessage = message
      println("IOPUB SENDING: " +
        zmqMessage.frames.map((byteString: ByteString) => new String(byteString.toArray)).mkString("\n"))
      socket ! zmqMessage
  }
}
