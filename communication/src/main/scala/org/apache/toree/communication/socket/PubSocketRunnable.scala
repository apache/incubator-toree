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

import org.zeromq.ZMQ.{Socket, Context}

/**
 * Represents the runnable component of a socket specifically targeted towards
 * publish sockets. No incoming messages are processed.
 *
 * @param context The ZMQ context to use with this runnable to create a socket
 * @param socketOptions The options to use when creating the socket
 */
class PubSocketRunnable(
  private val context: Context,
  private val socketOptions: SocketOption*
) extends ZeroMQSocketRunnable(
  context,
  PubSocket,
  None,
  socketOptions: _*
) {
  /** Does nothing. */
  override protected def processNextInboundMessage(
    socket: Socket,
    flags: Int
  ): Unit = {}
}
