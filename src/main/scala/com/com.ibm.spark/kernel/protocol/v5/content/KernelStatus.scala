package com.ibm.kernel.protocol.v5.content

import play.api.libs.json._

case class KernelStatus (
  execution_state: String
)

object KernelStatus {
  implicit val executeRequestReads = Json.reads[KernelStatus]
  implicit val executeRequestWrites = Json.writes[KernelStatus]
}