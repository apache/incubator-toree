package com.ibm.kernel.protocol.v5.content

import play.api.libs.json.Json

case class ClearOutput (
    matches: Boolean
)

object ClearOutput{
  implicit val clearHistoryReads = Json.reads[ClearOutput]
  implicit val clearHistoryWrites = Json.writes[ClearOutput]
}
