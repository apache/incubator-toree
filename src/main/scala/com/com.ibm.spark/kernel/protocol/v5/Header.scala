package com.ibm.kernel.protocol.v5

import play.api.libs.json._

case class Header(
  msg_id: UUID,    // IScala has a custom UUID type for this (protocol says UUID too)
  username: String,
  session: UUID,  // Custom UUID type also used here
  msg_type: String,
  version: String   // Assuming a string since the doc says '5.0'
)

object Header {
  implicit val headerReads = Json.reads[Header]
  implicit val headerWrites = Json.writes[Header]
}

/* LEFT FOR REFERENCE IN CREATING CUSTOM READ/WRITE
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
*/
