package com.ibm.kernel.protocol.v5.socket

import play.api.libs.json.Json

case class SocketConfig (
  stdin_port: Int,
  control_port: Int,
  hb_port: Int,
  shell_port: Int,
  iopub_port: Int,
  ip : String,
  transport: String,
  signature_scheme: String,
  key: String
)

object SocketConfig {
  implicit val socketConfigReads = Json.reads[SocketConfig]
  implicit val socketConfigWrites = Json.writes[SocketConfig]
}
