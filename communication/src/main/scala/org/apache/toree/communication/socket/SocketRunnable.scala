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
package org.apache.toree.communication.socket

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Represents the interface for a runnable used to send and receive messages
 * for a socket.
 *
 * @param inboundMessageCallback The callback to use when receiving a message
 *                               through this runnable
 */
abstract class SocketRunnable[T](
   private val inboundMessageCallback: Option[(Seq[Array[Byte]]) => Unit]
) extends Runnable {

  /** The collection of messages to be sent out through the socket. */
  val outboundMessages: ConcurrentLinkedQueue[T] =
    new ConcurrentLinkedQueue[T]()

  /**
   * Attempts to add a new message to the outbound queue to be sent out.
   *
   * @param message The message to add to the queue
   *
   * @return True if successfully queued the message, otherwise false
   */
  def offer(message: T): Boolean = outboundMessages.offer(message)

  /**
   * Indicates whether or not the runnable is processing messages (both
   * sending and receiving).
   *
   * @return True if processing, otherwise false
   */
  def isProcessing: Boolean

  /**
   * Closes the runnable such that it no longer processes messages and also
   * closes the underlying socket associated with the runnable.
   */
  def close(): Unit
}
