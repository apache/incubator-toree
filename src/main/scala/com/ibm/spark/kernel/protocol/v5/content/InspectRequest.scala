package com.ibm.spark.kernel.protocol.v5.content

import play.api.libs.json._

case class InspectRequest(
  code: String,
  cursor_pos: Int,
  detail_level: Int // TODO: This is currently either 0 or 1... should we
                    // TODO: look into enforcing that in our schema?
)

object InspectRequest {
  implicit val inspectRequestReads = Json.reads[InspectRequest]
  implicit val inspectRequestWrites = Json.writes[InspectRequest]
}

