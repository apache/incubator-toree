package com.ibm.spark.kernel.protocol.v5.content

import play.api.libs.json.Json

case class CompleteRequest(
  code: String,
  cursor_pos: Int
)

object CompleteRequest {
  implicit val completeRequestReads = Json.reads[CompleteRequest]
  implicit val completeRequestWrites = Json.writes[CompleteRequest]
}
