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

package org.apache.toree.kernel.protocol.v5.client.socket

import akka.actor.Actor
import akka.util.Timeout
import org.apache.toree.communication.ZMQMessage
import org.apache.toree.communication.security.SecurityActorType
import org.apache.toree.kernel.protocol.v5.client.{ActorLoader, Utilities}
import org.apache.toree.kernel.protocol.v5.{KernelMessage, UUID}
import Utilities._
import org.apache.toree.kernel.protocol.v5.client.execution.{DeferredExecution, DeferredExecutionManager}
import org.apache.toree.kernel.protocol.v5.content.ExecuteReply

import org.apache.toree.utils.LogLike
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern.ask

/**
 * The client endpoint for Shell messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 * @param actorLoader The loader used to retrieve actors
 * @param signatureEnabled Whether or not to check and provide signatures
 */
class ShellClient(
  socketFactory: SocketFactory,
  actorLoader: ActorLoader,
  signatureEnabled: Boolean
) extends Actor with LogLike {
  logger.debug("Created shell client actor")
  implicit val timeout = Timeout(21474835.seconds)

  val socket = socketFactory.ShellClient(context.system, self)

  def receiveExecuteReply(parentId:String, kernelMessage: KernelMessage): Unit = {
    val deOption: Option[DeferredExecution] = DeferredExecutionManager.get(parentId)
    deOption match {
      case None =>
        logger.warn(s"No deferred execution for parent id ${parentId}")
      case Some(de) =>
        Utilities.parseAndHandle(kernelMessage.contentString,
          ExecuteReply.executeReplyReads, (er: ExecuteReply) => de.resolveReply(er))
    }
  }

  override def receive: Receive = {
    // from shell
    case message: ZMQMessage =>
      logger.debug("Received shell kernel message.")
      val kernelMessage: KernelMessage = message

      // TODO: Validate incoming message signature

      logger.trace(s"Kernel message is ${kernelMessage}")
      receiveExecuteReply(message.parentHeader.msg_id,kernelMessage)

    // from handler
    case message: KernelMessage =>
      logger.trace(s"Sending kernel message ${message}")
      val signatureManager =
        actorLoader.load(SecurityActorType.SignatureManager)

      import scala.concurrent.ExecutionContext.Implicits.global
      val messageWithSignature = if (signatureEnabled) {
        val signatureMessage = signatureManager ? message
        Await.result(signatureMessage, 100.milliseconds)
          .asInstanceOf[KernelMessage]
      } else message

      val zMQMessage: ZMQMessage = messageWithSignature

      socket ! zMQMessage
  }
}
