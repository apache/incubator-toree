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

package org.apache.toree.kernel.protocol.v5.content

import org.apache.toree.kernel.protocol.v5.{KernelMessageContent, UserExpressions}
import play.api.libs.json._

case class ExecuteRequest(
  code: String,
  silent: Boolean,
  store_history: Boolean,
  user_expressions: UserExpressions,
  allow_stdin: Boolean
) extends KernelMessageContent {
  override def content : String =
    Json.toJson(this)(ExecuteRequest.executeRequestWrites).toString
}

object ExecuteRequest extends TypeString {
  implicit val executeRequestReads = Json.reads[ExecuteRequest]
  implicit val executeRequestWrites = Json.writes[ExecuteRequest]

  /**
   * Returns the type string associated with this object.
   *
   * @return The type as a string
   */
  override def toTypeString: String = "execute_request"
}

/* LEFT FOR REFERENCE IN CREATING CUSTOM READ/WRITE
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
*/

