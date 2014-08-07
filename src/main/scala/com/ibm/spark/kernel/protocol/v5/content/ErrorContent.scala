package com.ibm.spark.kernel.protocol.v5.content

import play.api.libs.json.Json

case class ErrorContent(
  ename: String,
  evalue: String,
  traceback: List[String]
)

object ErrorContent {
  implicit val errorContentReads = Json.reads[ErrorContent]
  implicit val errorContentWrites = Json.writes[ErrorContent]

  implicit def ErrorContentToString(errorContent: ErrorContent): String ={
    Json.toJson(errorContent).toString
  }
}