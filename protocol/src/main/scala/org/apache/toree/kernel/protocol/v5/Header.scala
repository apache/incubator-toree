/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package org.apache.toree.kernel.protocol.v5

import play.api.libs.json._

case class Header(
  msg_id: UUID,
  username: String,
  session: UUID,  // Custom UUID type also used here
  msg_type: String,
  version: String   // Assuming a string since the doc says '5.0'
)

object Header {
  implicit val headerReads = Json.reads[Header]
  //implicit val headerWrites = Json.writes[Header]
  implicit val headerWriters = new Writes[Header] {
    def writes(header: Header) =
      if (header != null) {
        Json.obj(
          "msg_id" -> header.msg_id,
          "username" -> header.username,
          "session" -> header.session,
          "msg_type" -> header.msg_type,
          "version" -> header.version
        )
      // Empty header is null
      } else {
        Json.obj()
      }
  }
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
