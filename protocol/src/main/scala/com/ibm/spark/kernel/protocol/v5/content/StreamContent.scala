package com.ibm.spark.kernel.protocol.v5.content

import play.api.libs.json._

case class StreamContent(
   name: String,
   text: String
)


object StreamContent {
  implicit val inspectRequestReads = Json.reads[StreamContent]
  implicit val inspectRequestWrites = Json.writes[StreamContent]
}

