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

package com.ibm.spark.kernel.protocol.v5.stream

import java.io.OutputStream
import java.nio.charset.Charset

import com.ibm.spark.kernel.protocol.v5.content.StreamContent
import com.ibm.spark.kernel.protocol.v5.{ActorLoader, KernelMessage, _}
import play.api.libs.json.Json

import scala.collection.mutable.ListBuffer

class KernelMessageStream(
  actorLoader: ActorLoader,
  kmBuilder: KMBuilder
) extends OutputStream {
  private val EncodingType = Charset.forName("UTF-8")
  private var internalBytes: ListBuffer[Byte] = ListBuffer()

  /**
   * Takes the current byte array contents in memory, packages them up into a
   * KernelMessage, and sends the message to the KernelMessageRelay.
   */
  override def flush(): Unit = {
    val contents = new String(internalBytes.toArray, EncodingType)
    val streamContent = StreamContent(
      "stdout", contents
    )

    val kernelMessage = kmBuilder
      .withIds(Seq(MessageType.Outgoing.Stream.toString))
      .withHeader(MessageType.Outgoing.Stream)
      .withContentString(streamContent).build

    actorLoader.load(SystemActorType.KernelMessageRelay) ! kernelMessage

    // Ensure any underlying implementation is processed
    super.flush()

    // Clear the internal buffer
    internalBytes.clear()
  }

  /**
   * Adds the specified byte to the end of the internal buffer. The most
   * significant 24 bits are ignored. Only the least significant 8 bits
   * are appended.
   * @param b The byte whose least significant 8 bits are to be appended
   */
  override def write(b: Int): Unit = {
    internalBytes += b.toByte

    // Attempt a flush if the provided byte was a newline
    if (b.toChar == '\n') flush()
  }
}
