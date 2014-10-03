package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.Actor
import akka.util.ByteString
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5.{ActorLoader, KernelMessage, SystemActorType}
import com.ibm.spark.utils.LogLike
import play.api.libs.json.Json
import com.ibm.spark.SparkKernelBootstrap
import java.nio.charset.Charset

/**
 * The server endpoint for shell messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class Shell(socketFactory: SocketFactory, actorLoader: ActorLoader) extends Actor with LogLike {
  logger.debug("Created new Shell actor")
  val socket = socketFactory.Shell(context.system, self)
  override def receive: Receive = {
    case message: ZMQMessage =>
      logger.debug("SHELL RECEIVING: " +
        message.frames.map((byteString: ByteString) => new String(byteString.toArray, Charset.forName("UTF-8"))).mkString("\n"))
//      val x =
//          SparkKernelBootstrap.hmac(
//            Json.stringify(Json.toJson(message.header)),
//            Json.stringify(Json.toJson(message.parentHeader)),
//            Json.stringify(Json.toJson(message.metadata)),
//            message.contentString
//          )
      actorLoader.load(SystemActorType.KernelMessageRelay) ! message

    case message: KernelMessage =>
      val zmqMessage: ZMQMessage = message
      logger.debug("SHELL SENDING: " +
        zmqMessage.frames.map((byteString: ByteString) => new String(byteString.toArray, Charset.forName("UTF-8"))).mkString("\n"))
      socket ! zmqMessage
  }
}
