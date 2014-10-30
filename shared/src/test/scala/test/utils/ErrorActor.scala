package test.utils

import akka.actor.Actor

class ErrorActor extends Actor {
  override def receive: Receive = {
    case message =>
      throw new RuntimeException
  }
}
