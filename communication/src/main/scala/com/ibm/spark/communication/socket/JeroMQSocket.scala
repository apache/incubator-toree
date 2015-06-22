package com.ibm.spark.communication.socket

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
  override def send(message: String*): Unit = {
    assert(isAlive, "Socket is not alive to be able to send messages!")

    runnable.offer(ZMsg.newStringMsg(message: _*))
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
