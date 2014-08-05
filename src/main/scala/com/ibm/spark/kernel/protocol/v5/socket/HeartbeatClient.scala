package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.{Actor, ActorLogging}
import akka.util.ByteString
import akka.zeromq.ZMQMessage

object HeartbeatMessage {}
/**
 * The client endpoint for heartbeat messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class HeartbeatClient(socketFactory : SocketFactory) extends Actor with ActorLogging {
  log.debug("Created new Heartbeat Client actor")
  val socket = socketFactory.HeartbeatClient(context.system, self)

  override def receive: Receive = {
    case message: ZMQMessage => println(message.toString)
    case HeartbeatMessage => socket ! ZMQMessage(ByteString("ping".getBytes))
  }
}