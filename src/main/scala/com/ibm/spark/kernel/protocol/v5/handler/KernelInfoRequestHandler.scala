package com.ibm.spark.kernel.protocol.v5.handler

import akka.actor.{Actor, ActorLogging}
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.KernelInfoReply
import play.api.libs.json.Json

/**
 * Receives a KernelInfoRequest KernelMessage and returns a KernelInfoReply
 * KernelMessage.
 */
class KernelInfoRequestHandler extends Actor with ActorLogging {
  override def receive: Receive = {
    case message: KernelMessage =>
      log.debug("Sending kernel info reply message")

      val kernelInfo = SparkKernelInfo
      val kernelInfoReply = KernelInfoReply(
        kernelInfo.protocol_version,
        kernelInfo.implementation,
        kernelInfo.implementation_version,
        kernelInfo.language,
        kernelInfo.language_version,
        kernelInfo.banner
      )

      val replyHeader = Header(
        java.util.UUID.randomUUID.toString,
        "",
        java.util.UUID.randomUUID.toString,
        "kernel_info_reply",
        kernelInfo.protocol_version
      )

      val kernelResponseMessage = new KernelMessage(
        Seq[String](),
        "",
        replyHeader,
        message.header,
        Metadata(),
        Json.toJson(kernelInfoReply).toString
      )
      sender ! kernelResponseMessage
  }
}