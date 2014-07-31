package com.ibm.kernel.protocol.v5.content

import com.ibm.kernel.protocol.v5.{Data, Metadata, UserExpressions}
import play.api.libs.json._

case class ExecuteInput(
  code: String,
  execution_count: Int
)

object ExecuteInput {
  implicit val executeInputReads = Json.reads[ExecuteInput]
  implicit val executeInputWrites = Json.writes[ExecuteInput]
}
