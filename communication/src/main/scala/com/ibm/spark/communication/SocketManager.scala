package com.ibm.spark.communication

import java.util.UUID
import com.ibm.spark.communication.socket._
import org.zeromq.ZMQ
import org.zeromq.ZMQ.Socket

class SocketManager {
  protected def newZmqContext(): ZMQ.Context = ZMQ.context(1)

  def newReqSocket(
    address: String,
    inboundMessageCallback: (Seq[String]) => Unit
  ): SocketLike = {
    new JeroMQSocket(new ZeroMQSocketRunnable(
      newZmqContext(),
      ReqSocket,
      Some(inboundMessageCallback),
      Connect(address),
      Linger(0)
    ){
      override protected def processNextInboundMessage(socket: Socket, flags: Int): Unit = {}

      override protected def processNextOutboundMessage(socket: Socket): Boolean = {
        val shouldReceiveMessage = super.processNextOutboundMessage(socket)

        if (shouldReceiveMessage) {
          super.processNextInboundMessage(socket, 0)
        }

        shouldReceiveMessage
      }
    })
  }

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

  def newPubSocket(
    address: String
  ): SocketLike = {
    new JeroMQSocket(new ZeroMQSocketRunnable(
      newZmqContext(),
      PubSocket,
      None,
      Bind(address),
      Linger(0)
    ){
      override protected def processNextInboundMessage(socket: Socket, flags: Int): Unit = {}
    })
  }

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
