package com.ibm.spark.kernel.protocol.v5.content

import com.ibm.spark.kernel.protocol.v5.{Data, Metadata}
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
