package com.ibm.spark.kernel.protocol.v5.content

import com.ibm.spark.kernel.protocol.v5.{Data, UUID}
import play.api.libs.json.Json

case class CommMsg(comm_id: UUID, data: Data)

object CommMsg {
  implicit val commOpenReads = Json.reads[CommMsg]
  implicit val commOpenWrites = Json.writes[CommMsg]
}
