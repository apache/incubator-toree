package com.ibm.spark.kernel.protocol.v5.socket

object SocketConnection {
  def apply(protocol: String, ip: String, port: Int) = new SocketConnection(protocol, ip, port)
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
