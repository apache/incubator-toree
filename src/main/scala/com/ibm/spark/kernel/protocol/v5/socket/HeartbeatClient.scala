package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.Actor
import akka.util.ByteString
import akka.zeromq.ZMQMessage
import com.ibm.spark.utils.LogLike

object HeartbeatMessage {}
/**
 * The client endpoint for heartbeat messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class HeartbeatClient(socketFactory : SocketFactory) extends Actor with LogLike {
  logger.debug("Created new Heartbeat Client actor")
  val socket = socketFactory.HeartbeatClient(context.system, self)

  override def receive: Receive = {
    case message: ZMQMessage => logger.trace(message.toString)
    case HeartbeatMessage => socket ! ZMQMessage(ByteString("ping".getBytes))
  }
}