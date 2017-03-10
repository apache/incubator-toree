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

import java.util.Calendar

import org.apache.toree.kernel.protocol.v5.MessageType.MessageType

/**
  * A class for building KernelMessages.
  * Upon creation, holds a KernelMessage with empty parameters. 
  * A new KMBuilder holding a KernelMessage with modified
  * parameters can be generated using the withXYZ() methods.
  * The KernelMessage held by the KMBuilder can be obtained with build().
  * All metadata returned by metadataDefaults() is added
  * to the KernelMessage's metadata when build() is called.
  *
  * e.g.
  * val base = KMBuilder().withParent(parentMessage)
  *
  * val km1 = base.withHeader(MessageType.ExecuteReply).build
  *
  * val km2 = base.withHeader(MessageType.ExecuteRequest)
  *               .withContentString("content").build
**/
case class KMBuilder(km: KernelMessage = KernelMessage(
                                            ids           = Seq(),
                                            signature     = "",
                                            header        = HeaderBuilder.empty,
                                            parentHeader  = HeaderBuilder.empty,
                                            metadata      = Metadata(),
                                            contentString = ""
                                            )) {
  require(km != null)

  def withIds(newVal: Seq[Array[Byte]]) : KMBuilder =
    KMBuilder(this.km.copy(ids = newVal))

  def withSignature(newVal: String) : KMBuilder =
    KMBuilder(this.km.copy(signature = newVal))

  def withHeader(newVal: Header) : KMBuilder =
    KMBuilder(this.km.copy(header = newVal))

  def withHeader(msgType: MessageType) : KMBuilder = {
    val header = HeaderBuilder.create(msgType.toString)
    KMBuilder(this.km.copy(header = header))
  }

  def withHeader(msgTypeString: String) : KMBuilder = {
    val header = HeaderBuilder.create(msgTypeString)
    KMBuilder(this.km.copy(header = header))
  }

  def withParent(parent: KernelMessage) : KMBuilder =
    KMBuilder(this.km.copy(parentHeader = parent.header))

  def withParentHeader(newVal: Header) : KMBuilder =
    KMBuilder(this.km.copy(parentHeader = newVal))

  def withMetadata(newVal: Metadata) : KMBuilder =
    KMBuilder(this.km.copy(metadata = newVal))

  def withContentString(newVal: String) : KMBuilder =
    KMBuilder(this.km.copy(contentString = newVal))

  def withContentString[T <: KernelMessageContent](contentMsg: T) : KMBuilder =
    KMBuilder(this.km.copy(contentString = contentMsg.content))

  /**
   * Default information (e.g. timestamp) to add to the
   * KernelMessage's metadata upon calling build().
   */
  protected def metadataDefaults : Metadata = {
    val timestamp = Calendar.getInstance().getTimeInMillis.toString
    Metadata("timestamp" -> timestamp)
  }

  /**
   * Builds a KernelMessage using the KernelMessage held by this builder.
   * @param includeDefaultMetadata appends default metadata (e.g. timestamp)
   *                               to the KernelMessage's metadata when true.
   * @return KernelMessage held by this builder with default metadata.
   */
  def build(implicit includeDefaultMetadata: Boolean = true) : KernelMessage = {
    if (includeDefaultMetadata)
      this.km.copy(metadata = km.metadata ++ metadataDefaults)
    else
      this.km.copy(metadata = km.metadata)
  }

}
