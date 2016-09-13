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

import akka.actor.{Actor, Props}
import org.apache.toree.interpreter.Interpreter
import org.apache.toree.kernel.protocol.v5.content.IsCompleteRequest
import org.apache.toree.utils.LogLike

object IsCompleteTaskActor {
  def props(interpreter: Interpreter): Props =
    Props(classOf[IsCompleteTaskActor], interpreter)
}

class IsCompleteTaskActor(interpreter: Interpreter)
  extends Actor with LogLike {
  require(interpreter != null)

  override def receive: Receive = {
    case req: IsCompleteRequest =>
      logger.debug("Invoking the interpreter completion")
      sender ! interpreter.isComplete(req.code)
    case _ =>
      sender ! "Unknown message" // TODO: Provide a failure message type to be passed around?
  }
}
