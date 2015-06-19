package com.ibm.spark.communication.socket

import org.zeromq.ZMsg

class JeroMQSocket(private val runnable: ZeroMQSocketRunnable)
  extends SocketLike {

  private val socketThread = new Thread(runnable)
  socketThread.start()

  override def send(message: String*): Unit = {
    runnable.offer(ZMsg.newStringMsg(message: _*))
  }

  override def close(): Unit = {
    runnable.close()
    socketThread.join()
  }
}
