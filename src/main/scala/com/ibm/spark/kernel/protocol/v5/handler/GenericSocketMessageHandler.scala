package com.ibm.spark.kernel.protocol.v5.handler

import akka.actor.{Actor, ActorLogging}
import com.ibm.spark.kernel.protocol.v5.SocketType.SocketType
import com.ibm.spark.kernel.protocol.v5.{ActorLoader, KernelMessage}

class GenericSocketMessageHandler(actorLoader: ActorLoader, socketType: SocketType) extends Actor with ActorLogging {
  override def receive: Receive = {
    case message: KernelMessage =>
      log.debug("Sending " + message.header.msg_id + "message to " + socketType.toString + "socket")
      actorLoader.loadSocketActor(socketType) ! message
  }

}
