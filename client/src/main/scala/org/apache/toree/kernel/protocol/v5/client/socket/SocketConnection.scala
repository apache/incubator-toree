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

object SocketConnection {
  def apply(protocol: String, ip: String, port: Int) =
    new SocketConnection(protocol, ip, port)
}

/**
 * Represent a connection string for a socket
 * @param protocol The protocol portion of the connection (e.g. tcp, akka, udp)
 * @param ip The hostname or ip address to bind on (e.g. *, myhost, 127.0.0.1)
 * @param port The port for the socket to listen on
 */
class SocketConnection(protocol: String, ip: String, port: Int) {
  private val SocketConnectionString : String = "%s://%s:%d"

  override def toString: String = {
    SocketConnectionString.format(protocol, ip, port)
  }
}
