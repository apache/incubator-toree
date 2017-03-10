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

package org.apache.toree.kernel.protocol.v5.stream

import java.io.OutputStream
import java.nio.charset.Charset

import org.apache.toree.kernel.protocol.v5.content.StreamContent
import org.apache.toree.kernel.protocol.v5.{SystemActorType, MessageType, KMBuilder}
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.utils.{LogLike, ScheduledTaskManager}
import scala.collection.mutable.ListBuffer
import KernelOutputStream._

object KernelOutputStream {
  val DefaultStreamType = "stdout"
  val DefaultSendEmptyOutput = false
}

/**
 * Represents an OutputStream that sends data back to the clients connect to the
 * kernel instance.
 *
 * @param actorLoader The actor loader used to access the message relay
 * @param kmBuilder The KMBuilder used to construct outgoing kernel messages
 * @param scheduledTaskManager The task manager used to schedule periodic
 *                             flushes to send data across the wire
 * @param streamType The type of stream (stdout/stderr)
 * @param sendEmptyOutput If true, will allow empty output to be flushed and
 *                        sent out to listening clients
 */
class KernelOutputStream(
  private val actorLoader: ActorLoader,
  private val kmBuilder: KMBuilder,
  private val scheduledTaskManager: ScheduledTaskManager,
  private val streamType: String = DefaultStreamType,
  private val sendEmptyOutput: Boolean = DefaultSendEmptyOutput
) extends OutputStream with LogLike {
  private val EncodingType = Charset.forName("UTF-8")
  @volatile private var internalBytes: ListBuffer[Byte] = ListBuffer()

  private var taskId: String = _

  private def enableAutoFlush() =
    if (taskId == null) {
      logger.trace("Enabling auto flush")
      taskId = scheduledTaskManager.addTask(task = this.flush())
    }

  private def disableAutoFlush() =
    if (taskId != null) {
      logger.trace("Disabling auto flush")
      scheduledTaskManager.removeTask(taskId)
      taskId = null
    }

  /**
   * Takes the current byte array contents in memory, packages them up into a
   * KernelMessage, and sends the message to the KernelMessageRelay.
   */
  override def flush(): Unit = {
    val contents = internalBytes.synchronized {
      logger.trace("Getting content to flush")
      val bytesToString = new String(internalBytes.toArray, EncodingType)

      // Clear the internal buffer
      internalBytes.clear()

      // Stop the auto-flushing
      disableAutoFlush()

      bytesToString
    }

    // Avoid building and sending a kernel message if the contents (when
    // trimmed) are empty and the flag to send anyway is disabled
    if (!sendEmptyOutput && contents.trim.isEmpty) {
      val contentsWithVisibleWhitespace = contents
        .replace("\n", "\\n")
        .replace("\t", "\\t")
        .replace("\r", "\\r")
        .replace(" ", "\\s")
      logger.warn(s"Suppressing empty output: '$contentsWithVisibleWhitespace'")
      return
    }

    logger.trace(s"Content to flush: '$contents'")

    val streamContent = StreamContent(
      streamType, contents
    )

    val kernelMessage = kmBuilder
      .withIds(Seq(MessageType.Outgoing.Stream.toString.getBytes))
      .withHeader(MessageType.Outgoing.Stream)
      .withContentString(streamContent).build

    actorLoader.load(SystemActorType.KernelMessageRelay) ! kernelMessage

    // Ensure any underlying implementation is processed
    super.flush()
  }

  /**
   * Adds the specified byte to the end of the internal buffer. The most
   * significant 24 bits are ignored. Only the least significant 8 bits
   * are appended.
   * @param b The byte whose least significant 8 bits are to be appended
   */
  override def write(b: Int): Unit = internalBytes.synchronized {
    // Begin periodic flushing if this is a new set of bytes
    enableAutoFlush()

    internalBytes += b.toByte
  }
}
