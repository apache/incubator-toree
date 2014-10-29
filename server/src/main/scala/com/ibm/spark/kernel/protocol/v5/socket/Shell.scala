package com.ibm.spark.kernel.protocol.v5.socket

import java.nio.charset.Charset

import akka.actor.Actor
import akka.util.ByteString
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5.{ActorLoader, KernelMessage, SystemActorType}
import com.ibm.spark.utils.LogLike
import com.ibm.spark.kernel.protocol.v5.Utilities._

/**
 * The server endpoint for shell messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class Shell(socketFactory: SocketFactory, actorLoader: ActorLoader) extends Actor with LogLike {
  logger.debug("Created new Shell actor")
  val socket = socketFactory.Shell(context.system, self)
  override def receive: Receive = {
    case message: ZMQMessage =>
      // Convert our ZMQ message to a kernel message
      val kernelMessage: KernelMessage = message

      logger.debug("SHELL RECEIVING: " +
        message.frames.map(
          (byteString: ByteString) =>
            new String(byteString.toArray, Charset.forName("UTF-8"))
        ).mkString("\n"))

      // Grab the strings to use for signature verification
      val zmqStrings = message.frames.map((byteString: ByteString) =>
        new String(byteString.toArray, Charset.forName("UTF-8"))
      ).takeRight(4) // TODO: This assumes NO extra buffers, refactor?

      // Forward along our message (along with the strings used for signatures)
      actorLoader.load(SystemActorType.KernelMessageRelay) !
        ((zmqStrings, kernelMessage))

    case message: KernelMessage =>
      // Convert our kernel message to a ZMQ message
      val zmqMessage: ZMQMessage = message

      logger.debug("SHELL SENDING: " +
        zmqMessage.frames.map(
          (byteString: ByteString) =>
            new String(byteString.toArray, Charset.forName("UTF-8"))
        ).mkString("\n"))

      // Forward along our message back through the socket
      socket ! zmqMessage
  }
}
