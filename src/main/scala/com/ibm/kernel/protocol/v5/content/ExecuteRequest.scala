package com.ibm.kernel.protocol.v5.content

import com.ibm.kernel.protocol.v5.UserExpressions
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class ExecuteRequest(
  code: String,
  silent: Boolean,
  storeHistory: Boolean,
  user_expressions: UserExpressions,
  allow_stdin: Boolean
)

object ExecuteRequest {
  implicit val headerReads: Reads[ExecuteRequest] = (
    (JsPath \ "code").read[String] and
    (JsPath \ "silent").read[Boolean] and
    (JsPath \ "store_history").read[Boolean] and
    (JsPath \ "user_expressions").read[UserExpressions] and
    (JsPath \ "allow_stdin").read[Boolean]
  )(ExecuteRequest.apply _) // Case class provides the apply method

  implicit val headerWrites: Writes[ExecuteRequest] = (
    (JsPath \ "code").write[String] and
    (JsPath \ "silent").write[Boolean] and
    (JsPath \ "store_history").write[Boolean] and
    (JsPath \ "user_expressions").write[UserExpressions] and
    (JsPath \ "allow_stdin").write[Boolean]
  )(unlift(ExecuteRequest.unapply)) // Case class provides the unapply method
}

