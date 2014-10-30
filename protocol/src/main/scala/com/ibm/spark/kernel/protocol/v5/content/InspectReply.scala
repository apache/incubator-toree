package com.ibm.spark.kernel.protocol.v5.content

// External libraries
import play.api.libs.json._

// Internal libraries
import com.ibm.spark.kernel.protocol.v5._

case class InspectReply(
  status: String,
  data: Data,
  metadata: Metadata,
  ename: Option[String],
  evalue: Option[String],
  traceback: Option[List[String]]
)

object InspectReply {
  implicit val inspectReplyOkReads = Json.reads[InspectReply]
  implicit val inspectReplyOkWrites = Json.writes[InspectReply]
}

