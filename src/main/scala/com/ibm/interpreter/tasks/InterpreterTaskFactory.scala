package com.ibm.interpreter.tasks

import akka.actor.{ActorSystem, ActorRef}
import com.ibm.interpreter.Interpreter

class InterpreterTaskFactory(interpreter: Interpreter) {

  /**
   * Creates a new actor representing this specific task.
   * @param system The actor system the task actor will belong
   * @return The ActorRef created for the task
   */
  def ExecuteRequestTask(system: ActorSystem): ActorRef =
    system.actorOf(ExecuteRequestTaskActor.props(interpreter))
}
