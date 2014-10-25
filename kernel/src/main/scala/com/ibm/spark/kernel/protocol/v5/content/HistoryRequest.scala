package com.ibm.spark.kernel.protocol.v5.content

import play.api.libs.json.Json

case class HistoryRequest(
  output: Boolean,
  ras: Boolean,
  hist_access_type: String,
  session: Int,
  start: Int,
  stop: Int,
  n: Int,
  pattern: String,
  unique: Boolean
)

object HistoryRequest {
  implicit val historyRequestReads = Json.reads[HistoryRequest]
  implicit val historyRequestWrites = Json.writes[HistoryRequest]
}
