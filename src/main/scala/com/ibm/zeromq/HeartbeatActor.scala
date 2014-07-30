package com.ibm.zeromq

import akka.actor.{Props, Actor}

object HeartbeatActor {
  def props: Props = Props(classOf[HeartbeatActor])
}

class HeartbeatActor extends Actor {
  override def receive: Receive = {
    case message => sender ! message
  }
}
