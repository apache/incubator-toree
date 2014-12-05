package com.ibm.spark.kernel.protocol.v5.socket

import java.nio.charset.Charset

import akka.actor.Actor
import akka.util.ByteString
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5.{ActorLoader, KernelMessage, SystemActorType}
import com.ibm.spark.kernel.protocol.v5.Utilities._
import com.ibm.spark.utils.{MessageLogSupport, LogLike}

/**
 * The server endpoint for shell messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class Shell(socketFactory: ServerSocketFactory, actorLoader: ActorLoader)
  extends Actor with MessageLogSupport{
  logger.trace("Created new Shell actor")
  val socket = socketFactory.Shell(context.system, self)
  override def receive: Receive = {
    case message: ZMQMessage =>
      val kernelMessage: KernelMessage = message
      logMessage(kernelMessage)

      // Grab the strings to use for signature verification
      val zmqStrings = message.frames.map((byteString: ByteString) =>
        new String(byteString.toArray, Charset.forName("UTF-8"))
      ).takeRight(4) // TODO: This assumes NO extra buffers, refactor?

      // Forward along our message (along with the strings used for signatures)
      actorLoader.load(SystemActorType.KernelMessageRelay) !
        ((zmqStrings, kernelMessage))

    case message: KernelMessage =>
      val zmqMessage: ZMQMessage = message
      logMessage(message)
      socket ! zmqMessage
  }
}
