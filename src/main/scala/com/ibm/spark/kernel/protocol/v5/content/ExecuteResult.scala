package com.ibm.spark.kernel.protocol.v5.content

import play.api.libs.json.Json
import com.ibm.spark.kernel.protocol.v5.{Metadata, Data}

case class ExecuteResult (
  execution_count: Int,
  data: Data,
  metadata: Metadata
) {
  def hasContent =
     data != null &&
     (data.get("text/plain") match {
       case Some(value) => value != null && value.nonEmpty
       case None => false
     })
}

object ExecuteResult {
  implicit val executeResultReads = Json.reads[ExecuteResult]
  implicit val executeResultWrites = Json.writes[ExecuteResult]
}