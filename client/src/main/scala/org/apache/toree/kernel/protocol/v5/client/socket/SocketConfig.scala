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

package org.apache.toree.kernel.protocol.v5.client.socket

import com.typesafe.config.Config
import play.api.libs.json.Json

case class SocketConfig (
  stdin_port: Int,
  control_port: Int,
  hb_port: Int,
  shell_port: Int,
  iopub_port: Int,
  ip : String,
  transport: String,
  signature_scheme: String,
  key: String
)

object SocketConfig {
  implicit val socketConfigReads = Json.reads[SocketConfig]
  implicit val socketConfigWrites = Json.writes[SocketConfig]

  def fromConfig(config: Config) = {
    new SocketConfig(
      config.getInt("stdin_port"),
      config.getInt("control_port"),
      config.getInt("hb_port"),
      config.getInt("shell_port"),
      config.getInt("iopub_port"),
      config.getString("ip"),
      config.getString("transport"),
      config.getString("signature_scheme"),
      config.getString("key")
    )
  }
}
