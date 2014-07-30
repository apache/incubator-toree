package com.ibm.kernel.protocol.v5.content

import com.ibm.kernel.protocol.v5.{Data, Metadata, UserExpressions}
import play.api.libs.json._

case class CodeInputs(
  code: String,
  execution_count: Int
)

object CodeInputs {
  implicit val executeRequestReads = Json.reads[CodeInputs]
  implicit val executeRequestWrites = Json.writes[CodeInputs]
}
