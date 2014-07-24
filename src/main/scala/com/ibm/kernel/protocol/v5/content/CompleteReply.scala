package com.ibm.kernel.protocol.v5.content

import com.ibm.kernel.protocol.v5.Metadata
import play.api.libs.json.Json

case class CompleteReply (matches: List[String], cursor_start: Int, cursor_end: Int, metadata: Metadata, status: String )

object CompleteReply{
  implicit val completeReplyReads = Json.reads[CompleteReply]
  implicit val completeReplyWrites = Json.writes[CompleteReply]
}
