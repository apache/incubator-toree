package com.ibm.spark.kernel.protocol.v5.interpreter.tasks

import akka.actor.{ActorRefFactory, ActorRef}
import com.ibm.spark.interpreter.Interpreter

class InterpreterTaskFactory(interpreter: Interpreter) {
  /**
   * Creates a new actor representing this specific task.
   * @param actorRefFactory The factory used to task actor will belong
   * @return The ActorRef created for the task
   */
  def ExecuteRequestTask(actorRefFactory: ActorRefFactory, name: String): ActorRef =
    actorRefFactory.actorOf(ExecuteRequestTaskActor.props(interpreter), name)
}
