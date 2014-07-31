package com.ibm.spark.kernel.protocol.v5.content

import play.api.libs.json.Json

case class KernelInfoReply (
  protocol_version: String,
  implementation: String,
  implementation_version: String,
  language: String,
  language_version: String,
  banner: String
)

object KernelInfoReply{
  implicit val kernelInfoReplyReads = Json.reads[KernelInfoReply]
  implicit val kernelInfoReplyWrites = Json.writes[KernelInfoReply]
}