package com.ibm.spark.kernel.protocol.v5.content

import com.ibm.spark.kernel.protocol.v5.{Data, Metadata}
import play.api.libs.json.Json

case class ExecuteResult (
  execution_count: Int,
  data: Data,
  metadata: Metadata
) {
  def hasContent = data != null && data.exists(x => x._2 != null && x._2.nonEmpty)
}

object ExecuteResult {
  implicit val executeResultReads = Json.reads[ExecuteResult]
  implicit val executeResultWrites = Json.writes[ExecuteResult]
}