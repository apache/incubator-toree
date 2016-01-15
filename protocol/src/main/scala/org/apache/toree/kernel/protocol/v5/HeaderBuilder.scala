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

object HeaderBuilder {
  /**
   * Creates a new Header instance with the provided id and type.
   * @param msgType The type of the message
   * @param msgId (Optional) The unique identifier of the message, generates a
   *              random UUID if none is provided
   * @return The new Header instance
   */
  def create(
    msgType: String,
    msgId: UUID = java.util.UUID.randomUUID.toString
  ) = Header(
      msgId,
      SparkKernelInfo.username,
      SparkKernelInfo.session,
      msgType,
      SparkKernelInfo.protocolVersion
    )

  /**
   * Represents an "empty" header where the message type and id are blank.
   */
  val empty = create("", "")
}
