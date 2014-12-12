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

import play.api.libs.json.Json

case class HistoryRequest(
  output: Boolean,
  ras: Boolean,
  hist_access_type: String,
  session: Int,
  start: Int,
  stop: Int,
  n: Int,
  pattern: String,
  unique: Boolean
)

object HistoryRequest {
  implicit val historyRequestReads = Json.reads[HistoryRequest]
  implicit val historyRequestWrites = Json.writes[HistoryRequest]
}
