package com.ibm.spark.kernel.protocol.v5.socket

import com.typesafe.config.Config
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

  def fromConfig(config: Config) = {
    new SocketConfig(
      config.getInt("stdin_port"),
      config.getInt("control_port"),
      config.getInt("hb_port"),
      config.getInt("shell_port"),
      config.getInt("iopub_port"),
      config.getString("ip"),
      config.getString("transport"),
      config.getString("signature_scheme"),
      config.getString("key")
    )
  }
}
