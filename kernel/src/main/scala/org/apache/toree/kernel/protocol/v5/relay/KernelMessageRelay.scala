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

package org.apache.toree.kernel.protocol.v5.relay

import akka.pattern.ask
import akka.util.Timeout
import org.apache.toree.communication.security.SecurityActorType
import org.apache.toree.communication.utils.OrderedSupport
import org.apache.toree.kernel.protocol.v5.MessageType.MessageType
import org.apache.toree.kernel.protocol.v5.content.ShutdownRequest
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.kernel.protocol.v5.{KernelMessage, MessageType, _}
import org.apache.toree.utils.MessageLogSupport

import scala.collection.immutable.HashMap
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success, Try}

/**
 * This class is meant to be a relay for send KernelMessages through kernel
 * system.
 * @param actorLoader The ActorLoader used by this class for finding actors for
 *                    relaying messages
 * @param incomingSpecialCases The special cases for incoming messages
 * @param outgoingSpecialCases The special cases for outgoing messages
 * @param useSignatureManager Whether or not to use signature verification and
 *                            generation
 */
case class KernelMessageRelay(
  actorLoader: ActorLoader,
  useSignatureManager: Boolean,
  incomingSpecialCases: Map[String, String] = new HashMap[String, String](),
  outgoingSpecialCases: Map[String, String] = new HashMap[String, String]()
) extends OrderedSupport with MessageLogSupport  {
  // NOTE: Required to provide the execution context for futures with akka
  import context._

  // NOTE: Required for ask (?) to function... maybe can define elsewhere?
  implicit val timeout = Timeout(5.seconds)

  // Flag indicating if can receive messages (or add them to buffer)
  var isReady = false

  def this(actorLoader: ActorLoader) =
    this(actorLoader, true)

  /**
   * Relays a KernelMessage to a specific actor to handle that message.
   *
   * @param messageType The enumeration representing the message type
   * @param kernelMessage The message to relay
   */
  private def relay(messageType: MessageType, kernelMessage: KernelMessage) = {
    logger.debug("Relaying message type of " + messageType.toString)
    logKernelMessageAction("Relaying", kernelMessage)
    actorLoader.load(messageType) ! kernelMessage
  }

  private def incomingRelay(kernelMessage: KernelMessage) = {
    var messageTypeString = kernelMessage.header.msg_type

    // If this is a special case, transform the message type accordingly
    if (incomingSpecialCases.contains(messageTypeString)) {
      logger.debug(s"$messageTypeString is a special incoming case!")
      messageTypeString = incomingSpecialCases(messageTypeString)
    }

    Try(MessageType.withName(messageTypeString)) match {
      case Success(messageName) => relay(messageName, kernelMessage)
      case Failure(_)           =>
        logger.warn(s"Ignoring unknown message type: $messageTypeString")
    }
  }

  private def outgoingRelay(kernelMessage: KernelMessage) = {
    var messageTypeString = kernelMessage.header.msg_type

    // If this is a special case, transform the message type accordingly
    if (outgoingSpecialCases.contains(messageTypeString)) {
      logger.debug(s"$messageTypeString is a special outgoing case!")
      messageTypeString = outgoingSpecialCases(messageTypeString)
    }

    Try(MessageType.withName(messageTypeString)) match {
      case Success(messageName) => relay(messageName, kernelMessage)
      case Failure(_)           =>
        logger.warn(s"Ignoring unknown message type: $messageTypeString")
    }
  }


  /**
   * This actor will receive and handle two types; ZMQMessage and KernelMessage.
   * These messages will be forwarded to the actors that are responsible for them.
   */
  override def receive = {
    // TODO: How to restore this when the actor dies?
    // Update ready status
    case ready: Boolean =>
      isReady = ready
      if (isReady) {
        logger.info("Unstashing all messages received!")
        unstashAll()
        logger.info("Relay is now fully ready to receive messages!")
      } else {
        logger.info("Relay is now disabled!")
      }


    // Add incoming messages (when not ready) to buffer to be processed
    case (zmqStrings: Seq[_], kernelMessage: KernelMessage) if !isReady && kernelMessage.header.msg_type != ShutdownRequest.toTypeString =>
      logger.info("Not ready for messages! Stashing until ready!")
      stash()

    // Assuming these messages are incoming messages
    case (zmqStrings: Seq[_], kernelMessage: KernelMessage) if isReady || kernelMessage.header.msg_type == ShutdownRequest.toTypeString =>
      startProcessing()
      if (useSignatureManager) {
        logger.trace(s"Verifying signature for incoming message " +
          s"${kernelMessage.header.msg_id}")
        val signatureManager =
          actorLoader.load(SecurityActorType.SignatureManager)
        val signatureVerificationFuture = signatureManager ? (
          (kernelMessage.signature, zmqStrings)
        )

        signatureVerificationFuture.mapTo[Boolean].onComplete {
          case Success(true) =>
            incomingRelay(kernelMessage)
            finishedProcessing()
          case Success(false) =>
            // TODO: Figure out what the failure message structure should be!
            logger.error(s"Invalid signature received from message " +
              s"${kernelMessage.header.msg_id}!")
            finishedProcessing()
          case Failure(t) =>
            logger.error("Failure when verifying signature!", t)
            finishedProcessing()
        }
      } else {
        logger.debug(s"Relaying incoming message " +
          s"${kernelMessage.header.msg_id} without SignatureManager")
        incomingRelay(kernelMessage)
        finishedProcessing()
      }

    // Assuming all kernel messages without zmq strings are outgoing
    case kernelMessage: KernelMessage =>
      startProcessing()
      if (useSignatureManager) {
        logger.trace(s"Creating signature for outgoing message " +
          s"${kernelMessage.header.msg_id}")
        val signatureManager = actorLoader.load(SecurityActorType.SignatureManager)
        val signatureInsertFuture = signatureManager ? kernelMessage

        // TODO: Handle error case for mapTo and non-present onFailure
        signatureInsertFuture.mapTo[KernelMessage] foreach {
          message =>
            outgoingRelay(message)
            finishedProcessing()
        }
      } else {
        logger.debug(s"Relaying outgoing message " +
          s"${kernelMessage.header.msg_id} without SignatureManager")
        outgoingRelay(kernelMessage)
        finishedProcessing()
      }
  }

  override def orderedTypes(): Seq[Class[_]] = Seq(
    classOf[(Seq[_], KernelMessage)],
    classOf[KernelMessage]
  )
}
