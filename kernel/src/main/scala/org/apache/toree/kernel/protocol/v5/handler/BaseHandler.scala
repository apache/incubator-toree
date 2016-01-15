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

package org.apache.toree.kernel.protocol.v5.handler

import org.apache.toree.communication.utils.OrderedSupport
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.utils.MessageLogSupport

import scala.concurrent.Future

abstract class BaseHandler(actorLoader: ActorLoader) extends OrderedSupport
  with MessageLogSupport {
  /**
   * Implements the receive method, sending a busy message out before
   * processing the message and sending an idle message out once finished.
   *
   * @return The Akka partial function n
   */
  final def receive = {
    // NOTE: Not using withProcessing as the finishedProcessing call is inside
    //       a future (not triggered immediately)
    case kernelMessage: KernelMessage =>
      startProcessing()
      // Send the busy message before we process the message
      logKernelMessageAction("Sending Busy message for", kernelMessage)
      actorLoader.load(SystemActorType.StatusDispatch) !
        Tuple2(KernelStatusType.Busy, kernelMessage.header)

      // Process the message
      logKernelMessageAction("Processing", kernelMessage)
      import scala.concurrent.ExecutionContext.Implicits.global
      val processFuture = process(kernelMessage)

      // Send the idle message since message has been processed
      logKernelMessageAction("Sending Idle message for", kernelMessage)
      processFuture onComplete {
        case _ =>
          actorLoader.load(SystemActorType.StatusDispatch) !
            Tuple2(KernelStatusType.Idle, kernelMessage.header)
          finishedProcessing()
      }
  }

  override def orderedTypes() : Seq[Class[_]] = {Seq(classOf[KernelMessage])}

  /**
   * Processes the provided kernel message.
   *
   * @param kernelMessage The kernel message instance to process
   */
  def process(kernelMessage: KernelMessage): Future[_]
}
