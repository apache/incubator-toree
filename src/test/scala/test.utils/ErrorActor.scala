package test.utils

import akka.actor.Actor

/**
 * Created by Chris on 8/13/14.
 */
class ErrorActor extends Actor {
  override def receive: Receive = {
    case message =>
      throw new RuntimeException
  }
}
