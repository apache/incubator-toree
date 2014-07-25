package com.ibm.kernel.protocol.v5.content


import com.ibm.kernel.protocol.v5.{Data, Metadata, UserExpressions}
import play.api.libs.json._

case class DisplayData(
  source: String,
  data: Data,
  metadata: Metadata
)

object DisplayData {
  implicit val executeRequestReads = Json.reads[DisplayData]
  implicit val executeRequestWrites = Json.writes[DisplayData]
}
