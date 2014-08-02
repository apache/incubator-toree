package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.{Actor, ActorLogging}
import akka.util.ByteString
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5.{SystemActorType, KernelMessage}

/**
 * The server endpoint for shell messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class Shell(socketFactory: SocketFactory) extends Actor with ActorLogging {
  log.debug("Created new Shell actor")
  val socket = socketFactory.Shell(context.system, self)
  override def receive: Receive = {
    case message: ZMQMessage =>
      println("SHELL RECEIVING: " +
        message.frames.map((byteString: ByteString) => new String(byteString.toArray)).mkString("\n"))
      context.actorSelection("/user/" + SystemActorType.Relay.toString) ! message
    case message: KernelMessage =>
      val zmqMessage: ZMQMessage = message
      log.debug("Sending shell message")
      println("SHELL SENDING: " +
        zmqMessage.frames.map((byteString: ByteString) => new String(byteString.toArray)).mkString("\n"))
      socket ! zmqMessage
  }
}
