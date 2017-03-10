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

import org.zeromq.ZMsg

/**
 * Represents a socket implemented using JeroMQ.
 *
 * @param runnable The underlying ZeroMQ socket runnable to use for the thread
 *                 managed by this socket
 */
class JeroMQSocket(private val runnable: ZeroMQSocketRunnable)
  extends SocketLike {

  private val socketThread = new Thread(runnable)
  socketThread.start()

  /**
   * Sends a message using this socket.
   *
   * @param message The message to send
   */
  override def send(message: Array[Byte]*): Unit = {
    assert(isAlive, "Socket is not alive to be able to send messages!")

    val msg = new ZMsg()
    for( frame <- message ) msg.add( frame )
    
    runnable.offer( msg )
  }

  /**
   * Closes the socket by closing the runnable and waiting for the underlying
   * thread to close.
   */
  override def close(): Unit = {
    runnable.close()
    socketThread.join()
  }

  /**
   * Indicates whether or not this socket is alive.
   *
   * @return True if alive (thread running), otherwise false
   */
  override def isAlive: Boolean = socketThread.isAlive

  /**
   * Indicates whether or not this socket is ready to send/receive messages.
   *
   * @return True if ready (runnable processing messages), otherwise false
   */
  override def isReady: Boolean = runnable.isProcessing
}
