package com.ibm.kernel.protocol.v5.content

import play.api.libs.json.Json

case class ConnectReply(
  shell_port: Int,
  iopub_port: Int,
  stdin_port: Int,
  hb_port: Int
)

object ConnectReply {
  implicit val connectReplyReads = Json.reads[ConnectReply]
  implicit val connectReplyWrites = Json.writes[ConnectReply]
}
