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

package org.apache.toree.kernel.protocol.v5.client.handler

import akka.actor.Actor
import akka.util.Timeout
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.client.{ActorLoader, Utilities}
import org.apache.toree.kernel.protocol.v5.client.execution.{ExecuteRequestTuple, DeferredExecutionManager}
import org.apache.toree.utils.LogLike
import scala.concurrent.duration._

/**
 * Actor for handling client execute request and reply messages
 */
class ExecuteHandler(actorLoader: ActorLoader) extends Actor with LogLike {
  implicit val timeout = Timeout(21474835.seconds)

  override def receive: Receive = {
    case reqTuple: ExecuteRequestTuple =>
      // create message to send to shell
      val km: KernelMessage = Utilities.toKernelMessage(reqTuple.request)
      //  Register the execution for this message id with the manager
      DeferredExecutionManager.add(km.header.msg_id,reqTuple.de)

      // send the message to the ShellClient
      val shellClient = actorLoader.load(SocketType.ShellClient)
      shellClient ! km
  }
}