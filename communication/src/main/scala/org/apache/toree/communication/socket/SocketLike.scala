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

/**
 * Represents a generic interface for socket communication.
 */
trait SocketLike {
  /**
   * Sends a message through the socket if alive.
   *
   * @throws AssertionError If the socket is not alive when attempting to send
   *                        a message
   *
   * @param message The message to send
   */
  def send(message: Array[Byte]*): Unit

  /**
   * Closes the socket, marking it no longer able to process or send messages.
   */
  def close(): Unit

  /**
   * Returns whether or not the socket is alive (processing new messages and
   * capable of sending out messages).
   *
   * @return True if alive, otherwise false
   */
  def isAlive: Boolean

  /**
   * Returns whether or not the socket is ready to send/receive messages.
   *
   * @return True if ready, otherwise false
   */
  def isReady: Boolean
}

