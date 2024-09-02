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

// External libraries

import org.apache.toree.kernel.protocol.v5.{KernelMessageContent, UserExpressions, Payloads}
import play.api.libs.json._

// Internal libraries
import scala.language.implicitConversions

case class ExecuteReply(
  status: String,
  execution_count: Int,
  payload: Option[Payloads],
  user_expressions: Option[UserExpressions],
  ename: Option[String],
  evalue: Option[String],
  traceback: Option[List[String]]
) extends KernelMessageContent with ReplyContent {

  override def content : String =
    Json.toJson(this)(ExecuteReply.executeReplyWrites).toString
}

object ExecuteReply extends TypeString {
  implicit val executeReplyReads = Json.reads[ExecuteReply]
  implicit val executeReplyWrites = Json.writes[ExecuteReply]

  implicit def ExecuteReplyToString(executeReply: ExecuteReply): String ={
      Json.toJson(executeReply).toString
  }

  /**
   * Returns the type string associated with this object.
   *
   * @return The type as a string
   */
  override def toTypeString: String = "execute_reply"
}




