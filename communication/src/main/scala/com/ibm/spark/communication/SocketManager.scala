package com.ibm.spark.communication

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import com.ibm.spark.communication.socket._
import org.zeromq.ZMQ

import scala.collection.JavaConverters._

/**
 * Represents the factory for sockets that also manages ZMQ contexts and
 * facilitates closing of sockets created by the factory.
 */
class SocketManager {
  /**
   * Creates a new ZMQ context with a single IO thread.
   *
   * @return The new ZMQ context
   */
  protected def newZmqContext(): ZMQ.Context = ZMQ.context(1)

  private val socketToContextMap =
    new ConcurrentHashMap[SocketLike, ZMQ.Context]().asScala

  /**
   * Closes the socket provided and also closes the context if no more sockets
   * are using the context.
   *
   * @param socket The socket to close
   */
  def closeSocket(socket: SocketLike) = {
    socket.close()
    socketToContextMap.remove(socket).foreach(context => {
      if (!socketToContextMap.values.exists(_ == context)) context.close()
    })
  }

  /**
   * Creates a new request socket.
   *
   * @param address The address to associate with the socket
   * @param inboundMessageCallback The callback to use for incoming messages
   *
   * @return The new socket instance
   */
  def newReqSocket(
    address: String,
    inboundMessageCallback: (Seq[String]) => Unit
  ): SocketLike = {
    new JeroMQSocket(new ReqSocketRunnable(
      newZmqContext(),
      Some(inboundMessageCallback),
      Connect(address),
      Linger(0)
    ))
  }

  /**
   * Creates a new reply socket.
   *
   * @param address The address to associate with the socket
   * @param inboundMessageCallback The callback to use for incoming messages
   *
   * @return The new socket instance
   */
  def newRepSocket(
    address: String,
    inboundMessageCallback: (Seq[String]) => Unit
  ): SocketLike = {
    new JeroMQSocket(new ZeroMQSocketRunnable(
      newZmqContext(),
      RepSocket,
      Some(inboundMessageCallback),
      Bind(address),
      Linger(0)
    ))
  }

  /**
   * Creates a new publish socket.
   *
   * @param address The address to associate with the socket
   *
   * @return The new socket instance
   */
  def newPubSocket(
    address: String
  ): SocketLike = {
    new JeroMQSocket(new PubSocketRunnable(
      newZmqContext(),
      Bind(address),
      Linger(0)
    ))
  }

  /**
   * Creates a new subscribe socket.
   *
   * @param address The address to associate with the socket
   * @param inboundMessageCallback The callback to use for incoming messages
   *
   * @return The new socket instance
   */
  def newSubSocket(
    address: String,
    inboundMessageCallback: (Seq[String]) => Unit
  ): SocketLike = {
    new JeroMQSocket(new ZeroMQSocketRunnable(
      newZmqContext(),
      SubSocket,
      Some(inboundMessageCallback),
      Connect(address),
      Linger(0),
      Subscribe.all
    ))
  }

  /**
   * Creates a new router socket.
   *
   * @param address The address to associate with the socket
   * @param inboundMessageCallback The callback to use for incoming messages
   *
   * @return The new socket instance
   */
  def newRouterSocket(
    address: String,
    inboundMessageCallback: (Seq[String]) => Unit
  ): SocketLike = {
    new JeroMQSocket(new ZeroMQSocketRunnable(
      newZmqContext(),
      RouterSocket,
      Some(inboundMessageCallback),
      Bind(address),
      Linger(0)
    ))
  }

  /**
   * Creates a new dealer socket.
   *
   * @param address The address to associate with the socket
   * @param inboundMessageCallback The callback to use for incoming messages
   *
   * @return The new socket instance
   */
  def newDealerSocket(
    address: String,
    inboundMessageCallback: (Seq[String]) => Unit,
    identity: String = UUID.randomUUID().toString
  ): SocketLike = {
    new JeroMQSocket(new ZeroMQSocketRunnable(
      newZmqContext(),
      DealerSocket,
      Some(inboundMessageCallback),
      Connect(address),
      Linger(0),
      Identity(identity.getBytes(ZMQ.CHARSET))
    ))
  }
}
