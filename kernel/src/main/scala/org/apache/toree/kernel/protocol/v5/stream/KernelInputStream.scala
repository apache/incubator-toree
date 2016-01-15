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

import java.io.InputStream
import java.nio.charset.Charset

import akka.pattern.ask
import org.apache.toree.kernel.protocol.v5.content.InputRequest
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.kernel.protocol.v5.kernel.Utilities.timeout
import org.apache.toree.kernel.protocol.v5.{KMBuilder, MessageType}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, Future}

import KernelInputStream._

object KernelInputStream {
  val DefaultPrompt = ""
  val DefaultPassword = false
}

/**
 * Represents an OutputStream that sends data back to the clients connect to the
 * kernel instance.
 *
 * @param actorLoader The actor loader used to access the message relay
 * @param kmBuilder The KMBuilder used to construct outgoing kernel messages
 * @param prompt The prompt to use for input requests
 * @param password Whether or not the input request is for a password
 */
class KernelInputStream(
  actorLoader: ActorLoader,
  kmBuilder: KMBuilder,
  prompt: String = DefaultPrompt,
  password: Boolean = DefaultPassword
) extends InputStream {
  private val EncodingType = Charset.forName("UTF-8")
  @volatile private var internalBytes: ListBuffer[Byte] = ListBuffer()

  /**
   * Returns the number of bytes available before the next request is made
   * for more data.
   * @return The total number of bytes in the internal buffer
   */
  override def available(): Int = internalBytes.length

  /**
   * Requests the next byte of data from the client. If the buffer containing
   * @return The byte of data as an integer
   */
  override def read(): Int = {
    if (!this.hasByte) this.requestBytes()

    this.nextByte()
  }

  private def hasByte: Boolean = internalBytes.nonEmpty

  private def nextByte(): Int = {
    val byte = internalBytes.head

    internalBytes = internalBytes.tail

    byte
  }

  private def requestBytes(): Unit = {
    val inputRequest = InputRequest(prompt, password)
    // NOTE: Assuming already provided parent header and correct ids
    val kernelMessage = kmBuilder
      .withHeader(MessageType.Outgoing.InputRequest)
      .withContentString(inputRequest)
      .build

    // NOTE: The same handler is being used in both request and reply
    val responseFuture: Future[String] =
      (actorLoader.load(MessageType.Incoming.InputReply) ? kernelMessage)
      .mapTo[String]

    // Block until we get a response
    import scala.concurrent.duration._
    internalBytes ++=
      Await.result(responseFuture, Duration.Inf).getBytes(EncodingType)
  }
}
