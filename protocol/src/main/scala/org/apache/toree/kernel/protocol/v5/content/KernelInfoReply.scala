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

import org.apache.toree.kernel.protocol.v5.KernelMessageContent
import org.apache.toree.kernel.protocol.v5.LanguageInfo
import play.api.libs.json.Json

case class KernelInfoReply (
  protocol_version: String,
  implementation: String,
  implementation_version: String,
  language_info: LanguageInfo,
  banner: String
) extends KernelMessageContent {
  override def content: String =
    Json.toJson(this)(KernelInfoReply.kernelInfoReplyWrites).toString
}

object KernelInfoReply extends TypeString {
  implicit val languageInfoReads = Json.reads[LanguageInfo]
  implicit val languageInfoWrites = Json.writes[LanguageInfo]
  implicit val kernelInfoReplyReads = Json.reads[KernelInfoReply]
  implicit val kernelInfoReplyWrites = Json.writes[KernelInfoReply]

  /**
   * Returns the type string associated with this object.
   *
   * @return The type as a string
   */
  override def toTypeString: String = "kernel_info_reply"
}
