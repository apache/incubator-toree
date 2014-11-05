package com.ibm.spark.kernel.protocol.v5.content

import com.ibm.spark.kernel.protocol.v5.{Data, UUID}
import play.api.libs.json.Json

case class CommOpen(comm_id: UUID, target_name: String, data: Data)

object CommOpen {
  implicit val commOpenReads = Json.reads[CommOpen]
  implicit val commOpenWrites = Json.writes[CommOpen]
}
