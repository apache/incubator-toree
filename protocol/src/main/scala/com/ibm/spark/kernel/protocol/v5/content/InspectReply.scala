/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.spark.kernel.protocol.v5.content

// External libraries
import play.api.libs.json._

// Internal libraries
import com.ibm.spark.kernel.protocol.v5._

case class InspectReply(
  status: String,
  data: Data,
  metadata: Metadata,
  ename: Option[String],
  evalue: Option[String],
  traceback: Option[List[String]]
)

object InspectReply {
  implicit val inspectReplyOkReads = Json.reads[InspectReply]
  implicit val inspectReplyOkWrites = Json.writes[InspectReply]
}

