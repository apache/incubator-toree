package com.ibm.spark.kernel.protocol.v5.handler

import akka.actor.Actor
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.KernelInfoReply
import com.ibm.spark.utils.LogLike
import play.api.libs.json.Json

/**
 * Receives a KernelInfoRequest KernelMessage and returns a KernelInfoReply
 * KernelMessage.
 */
class KernelInfoRequestHandler(actorLoader: ActorLoader) extends Actor with LogLike {
  override def receive: Receive = {
    case message: KernelMessage =>
      logger.debug("Sending kernel info reply message")

      val kernelInfo = SparkKernelInfo
      val kernelInfoReply = KernelInfoReply(
        kernelInfo.protocolVersion,
        kernelInfo.implementation,
        kernelInfo.implementationVersion,
        kernelInfo.language,
        kernelInfo.languageVersion,
        kernelInfo.banner
      )

      val replyHeader = Header(
        java.util.UUID.randomUUID.toString,
        "",
        java.util.UUID.randomUUID.toString,
        MessageType.KernelInfoReply.toString,
        kernelInfo.protocolVersion
      )

      val kernelResponseMessage = new KernelMessage(
        message.ids,
        "",
        replyHeader,
        message.header,
        Metadata(),
        Json.toJson(kernelInfoReply).toString
      )

      actorLoader.load(SystemActorType.KernelMessageRelay) ! kernelResponseMessage
  }
}