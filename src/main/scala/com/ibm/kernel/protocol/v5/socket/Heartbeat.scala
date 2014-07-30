package com.ibm.kernel.protocol.v5.socket

import akka.actor.Actor

class Heartbeat extends Actor {
  override def receive: Receive = {
    case message => sender ! message
  }
}
