package com.ibm.spark.kernel.protocol.v5.content

import play.api.libs.json.Json

case class ClearHistory (
    matches: Boolean
)

object ClearHistory{
  implicit val clearHistoryReads = Json.reads[ClearHistory]
  implicit val clearHistoryWrites = Json.writes[ClearHistory]
}
