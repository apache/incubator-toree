package com.ibm.spark.kernel.protocol.v5.content

import play.api.libs.json.Json

case class ExecuteResult (
  execution_count: Int,
  data: Data,
  metadata: Metadata
)

object ExecuteResult {
  implicit val executeResultReads = Json.reads[ExecuteResult]
  implicit val executeResultWrites = Json.writes[ExecuteResult]
}