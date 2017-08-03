/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package org.apache.toree.kernel.protocol.v5.interpreter.tasks

import akka.actor.{ActorRef, ActorRefFactory}
import org.apache.toree.interpreter.Interpreter
import org.apache.toree.kernel.api.KernelLike

class InterpreterTaskFactory(kernel: KernelLike, interpreter: Interpreter) {
  /**
   * Creates a new actor representing this specific task.
   * @param actorRefFactory The factory used to task actor will belong
   * @return The ActorRef created for the task
   */
  def ExecuteRequestTask(actorRefFactory: ActorRefFactory, name: String): ActorRef =
    actorRefFactory.actorOf(ExecuteRequestTaskActor.props(kernel, interpreter), name)

  /**
   *
   */
  def CodeCompleteTask(actorRefFactory: ActorRefFactory, name: String): ActorRef =
    actorRefFactory.actorOf(CodeCompleteTaskActor.props(interpreter), name)


  def IsCompleteTask(actorRefFactory: ActorRefFactory, name: String): ActorRef =
    actorRefFactory.actorOf(IsCompleteTaskActor.props(interpreter), name)
}
