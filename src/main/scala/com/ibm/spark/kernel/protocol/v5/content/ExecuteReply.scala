package com.ibm.spark.kernel.protocol.v5.content

// External libraries
import play.api.libs.json._

// Internal libraries
import com.ibm.spark.kernel.protocol.v5._

case class ExecuteReply(
  status: String,
  execution_count: Int,
  payload: Option[Payloads],
  user_expressions: Option[UserExpressions],
  ename: Option[String],
  evalue: Option[String],
  traceback: Option[List[String]]
)

object ExecuteReply {
  implicit val executeReplyOkReads = Json.reads[ExecuteReply]
  implicit val executeReplyOkWrites = Json.writes[ExecuteReply]

  implicit def ExecuteReplyToString(executeReply: ExecuteReply): String ={
      Json.toJson(executeReply).toString
  }
}




