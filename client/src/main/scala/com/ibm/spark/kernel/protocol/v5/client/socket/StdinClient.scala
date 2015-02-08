/*
 * Copyright 2015 IBM Corp.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.ibm.spark.kernel.protocol.v5.client.socket

import akka.actor.Actor
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5.{HeaderBuilder, KMBuilder, KernelMessage}
import com.ibm.spark.kernel.protocol.v5.content.{InputReply, InputRequest}
import com.ibm.spark.utils.LogLike
import com.ibm.spark.kernel.protocol.v5.client.Utilities._
import play.api.libs.json.Json

import StdinClient._

object StdinClient {
  type ResponseFunction = (String, Boolean) => String
  val EmptyResponseFunction: ResponseFunction = (_, _) => ""
}

/**
 * The client endpoint for Stdin messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class StdinClient(
  socketFactory: SocketFactory
) extends Actor with LogLike {
  logger.debug("Created stdin client actor")

  private val socket = socketFactory.StdinClient(context.system, self)

  /**
   * The function to use for generating a response from an input_request
   * message.
   */
  private var responseFunc: ResponseFunction = EmptyResponseFunction

  override def receive: Receive = {
    case responseFunc: ResponseFunction =>
      logger.debug("Updating response function")
      this.responseFunc = responseFunc

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

        val responseZmqMessage: ZMQMessage = KMBuilder()
          .withParent(kernelMessage)
          .withHeader(HeaderBuilder.empty.copy(
            msg_type = InputReply.toTypeString,
            session = getSessionId
          ))
          .withContentString(inputReply)
          .build

        socket ! responseZmqMessage
      } else {
        logger.debug(s"Unknown message of type $messageType")
      }
  }
}
