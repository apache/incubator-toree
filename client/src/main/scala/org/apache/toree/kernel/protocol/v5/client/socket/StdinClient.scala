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
import org.apache.toree.communication.ZMQMessage
import org.apache.toree.communication.security.SecurityActorType
import org.apache.toree.kernel.protocol.v5.client.ActorLoader
import org.apache.toree.kernel.protocol.v5.{HeaderBuilder, KMBuilder, KernelMessage}
import org.apache.toree.kernel.protocol.v5.content.{InputReply, InputRequest}
import org.apache.toree.utils.LogLike
import org.apache.toree.kernel.protocol.v5.client.Utilities._
import play.api.libs.json.Json

import StdinClient._
import akka.pattern.ask

import scala.concurrent.duration._
import scala.concurrent.Await

object StdinClient {
  case class ResponseFunctionMessage(responseFunction: ResponseFunction)
  type ResponseFunction = (String, Boolean) => String
  val EmptyResponseFunction: ResponseFunction = (_, _) => ""
}

/**
 * The client endpoint for Stdin messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 * @param actorLoader The loader used to retrieve actors
 * @param signatureEnabled Whether or not to check and provide signatures
 */
class StdinClient(
  socketFactory: SocketFactory,
  actorLoader: ActorLoader,
  signatureEnabled: Boolean
) extends Actor with LogLike {
  logger.debug("Created stdin client actor")

  private val socket = socketFactory.StdinClient(context.system, self)

  /**
   * The function to use for generating a response from an input_request
   * message.
   */
  private var responseFunc: ResponseFunction = EmptyResponseFunction

  override def receive: Receive = {
    case responseFunctionMessage: ResponseFunctionMessage =>
      logger.debug("Updating response function")
      this.responseFunc = responseFunctionMessage.responseFunction

    case message: ZMQMessage =>
      logger.debug("Received stdin kernel message")
      val kernelMessage: KernelMessage = message
      val messageType = kernelMessage.header.msg_type

      if (messageType == InputRequest.toTypeString) {
        logger.debug("Message is an input request")

        val inputRequest =
          Json.parse(kernelMessage.contentString).as[InputRequest]
        val value = responseFunc(inputRequest.prompt, inputRequest.password)
        val inputReply = InputReply(value)

        val newKernelMessage = KMBuilder()
          .withParent(kernelMessage)
          .withHeader(HeaderBuilder.empty.copy(
            msg_type = InputReply.toTypeString,
            session = getSessionId
          ))
          .withContentString(inputReply)
          .build

        import scala.concurrent.ExecutionContext.Implicits.global
        val messageWithSignature = if (signatureEnabled) {
          val signatureManager =
            actorLoader.load(SecurityActorType.SignatureManager)
          val signatureMessage = signatureManager ? newKernelMessage
          Await.result(signatureMessage, 100.milliseconds)
            .asInstanceOf[KernelMessage]
        } else newKernelMessage

        val zmqMessage: ZMQMessage = messageWithSignature

        socket ! zmqMessage
      } else {
        logger.debug(s"Unknown message of type $messageType")
      }
  }
}
