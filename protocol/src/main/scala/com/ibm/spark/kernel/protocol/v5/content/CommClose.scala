package com.ibm.spark.kernel.protocol.v5.content

import com.ibm.spark.kernel.protocol.v5.{Data, UUID}
import play.api.libs.json.Json

case class CommClose(comm_id: UUID, data: Data)

object CommClose {
  implicit val commOpenReads = Json.reads[CommClose]
  implicit val commOpenWrites = Json.writes[CommClose]
}
