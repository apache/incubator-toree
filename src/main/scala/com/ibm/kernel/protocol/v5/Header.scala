package com.ibm.kernel.protocol.v5

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Header(
  msgId: String,    // IScala has a custom UUID type for this (protocol says UUID too)
  username: String,
  session: String,  // Custom UUID type also used here
  msgType: String,
  version: String   // Assuming a string since the doc says '5.0'
)

object Header {
  implicit val headerReads: Reads[Header] = (
    (JsPath \ "msg_id").read[String] and
    (JsPath \ "username").read[String] and
    (JsPath \ "session").read[String] and
    (JsPath \ "msg_type").read[String] and
    (JsPath \ "version").read[String]
  )(Header.apply _) // Case class provides the apply method

  implicit val headerWrites: Writes[Header] = (
    (JsPath \ "msg_id").write[String] and
    (JsPath \ "username").write[String] and
    (JsPath \ "session").write[String] and
    (JsPath \ "msg_type").write[String] and
    (JsPath \ "version").write[String]
  )(unlift(Header.unapply)) // Case class provides the unapply method
}
