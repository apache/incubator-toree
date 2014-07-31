package com.ibm.spark.kernel.protocol.v5.handler

import akka.actor.{ActorLogging, Actor}
import com.ibm.spark.kernel.protocol.v5.{SparkKernelInfo, Header, KernelMessage}
import com.ibm.spark.kernel.protocol.v5.content.{KernelInfoReply, KernelInfoRequest}
import play.api.libs.json.Json
import com.ibm.spark.kernel.protocol.v5._

/**
 * Receives a KernelInfoRequest KernelMessage and returns a KernelInfoResponse
 * KernelMessage.
 */
class KernelInfoRequestHandler extends Actor with ActorLogging {
  override def receive: Receive = {
    case message: KernelMessage =>
      log.debug("Sending kernel info response message")

      //val kernelInfoRequest = Json.parse(message.contentString).as[KernelInfoRequest]
      val kernelInfo = SparkKernelInfo
      val kernelInfoReply = KernelInfoReply(
        kernelInfo.protocol_version,
        kernelInfo.implementation,
        kernelInfo.implementation_version,
        kernelInfo.language,
        kernelInfo.language_version,
        kernelInfo.banner
      )

      val responseHeader = Header(
        java.util.UUID.randomUUID.toString,
        "",
        java.util.UUID.randomUUID.toString,
        "kernel_info_reply",
        kernelInfo.protocol_version
      )

      val kernelResponseMessage = new KernelMessage(
        Seq[String](),
        "",
        responseHeader,
        message.header,
        Metadata(),
        Json.toJson(kernelInfoReply).toString
      )
      sender ! kernelResponseMessage
    case _ =>
      sender ! "Error: Expected KernelMessage"
  }
}