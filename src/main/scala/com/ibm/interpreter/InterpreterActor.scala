package com.ibm.interpreter

import akka.actor.{ActorRef, Props, Actor}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.ibm.interpreter.tasks._
import com.ibm.kernel.protocol.v5._
import com.ibm.kernel.protocol.v5.content._

import scala.concurrent.duration._

object InterpreterActor {
  def props(interpreter: Interpreter): Props =
    Props(classOf[InterpreterActor], interpreter)
}

// TODO: Investigate restart sequence
//
// http://doc.akka.io/docs/akka/2.2.3/general/supervision.html
//
// "create new actor instance by invoking the originally provided factory again"
//
// Does this mean that the interpreter instance is not gc and is passed in?
//
class InterpreterActor(interpreter: Interpreter) extends Actor {
  require(interpreter != null)

  // NOTE: Required to provide the execution context for futures with akka
  import context._

  // NOTE: Required for ask (?) to function... maybe can define elsewhere?
  implicit val timeout = Timeout(5 seconds)

  //
  // List of child actors that the interpreter contains
  //
  private var executeRequestTask: ActorRef = _

  /**
   * Initializes all child actors performing tasks for the interpreter.
   */
  override def preStart = {
    executeRequestTask =
      context.actorOf(ExecuteRequestTaskActor.props(interpreter))
  }
  
  override def receive: Receive = {
    case executeRequest: ExecuteRequest =>
      (executeRequestTask ? executeRequest) recover {
        case ex: Exception => // TODO: Provide failure message type to be passed around?
      } pipeTo sender
    case _ =>
      sender ! "Unknown message" // TODO: Provide a failure message type to be passed around?
  }
}
