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

import org.zeromq.ZMQ

/** Represents an option to provide to a socket. */
sealed trait SocketOption

/**
 * Represents the linger option used to communicate the millisecond duration
 * to continue processing messages after the socket has been told to close.
 *
 * @note Provide -1 as the duration to wait until all messages are processed
 *
 * @param milliseconds The duration in milliseconds
 */
case class Linger(milliseconds: Int) extends SocketOption

/**
 * Represents the subscribe option used to filter messages coming into a
 * socket subscribing to a publisher. Uses the provided byte prefix to filter
 * incoming messages.
 *
 * @param topic The array of bytes to use as a filter based on the
 *              bytes at the beginning of incoming messages
 */
case class Subscribe(topic: Array[Byte]) extends SocketOption
object Subscribe {
  val all = Subscribe(ZMQ.SUBSCRIPTION_ALL)
}

/**
 * Represents the identity option used to identify the socket.
 *
 * @param identity The identity to use with the socket
 */
case class Identity(identity: Array[Byte]) extends SocketOption

/**
 * Represents the bind option used to tell the socket what address to bind to.
 *
 * @param address The address for the socket to use
 */
case class Bind(address: String) extends SocketOption

/**
 * Represents the connect option used to tell the socket what address to
 * connect to.
 *
 * @param address The address for the socket to use
 */
case class Connect(address: String) extends SocketOption
