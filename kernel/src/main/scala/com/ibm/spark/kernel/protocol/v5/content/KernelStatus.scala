package com.ibm.spark.kernel.protocol.v5.content

import play.api.libs.json._

case class KernelStatus (
  execution_state: String
)

object KernelStatus {
  implicit val executeRequestReads = Json.reads[KernelStatus]
  implicit val executeRequestWrites = Json.writes[KernelStatus]
}

object KernelStatusBusy extends KernelStatus("busy") {
  override def toString(): String = {
    Json.toJson(this).toString
  }
}
object KernelStatusIdle extends KernelStatus("idle") {
  override def toString(): String = {
    Json.toJson(this).toString
  }
}