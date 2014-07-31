package com.ibm.kernel.protocol.v5.content

import play.api.libs.json.Json

case class ShutdownRequest(
  restart: Boolean
)

object ShutdownRequest {
  implicit val completeRequestReads = Json.reads[ShutdownRequest]
  implicit val completeRequestWrites = Json.writes[ShutdownRequest]
}
