package com.ibm.kernel.protocol.v5.content

import play.api.libs.json.Json


case class HistoryReply(
  // TODO: This is really (String, String, String | (String, String)), look
  // TODO: into writing implicits to handle tuples

  // NOTE: Currently, only handle (String, String, String)
  history: List[String]
)

object HistoryReply {
  implicit val historyReplyReads = Json.reads[HistoryReply]
  implicit val historyReplyWrites = Json.writes[HistoryReply]
}
