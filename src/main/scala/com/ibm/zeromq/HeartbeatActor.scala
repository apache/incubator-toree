package com.ibm.zeromq

import akka.actor.Actor

class HeartbeatActor extends Actor {
  override def receive: Receive = {
    case message => sender ! message
  }
}
