package com.ibm.spark.kernel.protocol.v5.relay

import java.util.UUID

import akka.actor.Actor
import akka.pattern.ask
import akka.util.Timeout
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5.MessageType.MessageType
import com.ibm.spark.kernel.protocol.v5.{ActorLoader, KernelMessage, MessageType, SystemActorType}
import com.ibm.spark.utils.LogLike

import scala.concurrent.duration._

/**
 * This class is meant to be a relay for send KernelMessages through kernel system.
 * @param actorLoader The ActorLoader used by this class for finding actors for relaying messages
 */
case class KernelMessageRelay(
  actorLoader: ActorLoader, incomingMessageMap: Map[String, String],
  useSignatureManager: Boolean
) extends Actor with LogLike {
  // NOTE: Required to provide the execution context for futures with akka
  import context._

  // NOTE: Required for ask (?) to function... maybe can define elsewhere?
  implicit val timeout = Timeout(5.seconds)

  var isReady = false

  def this(actorLoader: ActorLoader, incomingMessageMap: Map[String, String]) =
    this(actorLoader, incomingMessageMap, true)

  /**
   * Relays a KernelMessage to a specific actor to handle that message
   * @param kernelMessage The message to relay
   */
  private def relay(kernelMessage: KernelMessage){
    val messageType: MessageType = MessageType.withName(kernelMessage.header.msg_type)
    logger.info("IS_READY: " + isReady)
    logger.info("Relaying message of type " + kernelMessage.header.msg_type )
    actorLoader.load(messageType) ! kernelMessage
  }

  /**
   * This actor will receive and handle two types; ZMQMessage and KernelMessage. These messages
   * will be forwarded to their actors which are responsible for them.
   */
  override def receive = {
    // TODO: How to restore this when the actor dies?
    case ready: Boolean =>
      isReady = ready
      logger.info("Relay is now fully ready to receive messages!")

    // Assuming these messages are incoming messages
    case kernelMessage: KernelMessage
      if incomingMessageMap.contains(kernelMessage.header.msg_type) && isReady =>
        if (useSignatureManager) {
          val signatureManager = actorLoader.load(SystemActorType.SignatureManager)
          val signatureVerificationFuture = signatureManager ? kernelMessage

          // TODO: Handle error case for mapTo and non-present onFailure
          signatureVerificationFuture.mapTo[Boolean] onSuccess {
            // Verification successful, so continue relay
            case true => relay(kernelMessage)

            // TODO: Figure out what the failure message structure should be!
            // Verification failed, so report back a failure
            case false => // sender ! SOME MESSAGE
          }
        } else {
          relay(kernelMessage)
        }

    // Assuming all other kernel messages are outgoing
    case kernelMessage: KernelMessage
      if !incomingMessageMap.contains(kernelMessage.header.msg_type) =>
        if (useSignatureManager) {
          val signatureManager = actorLoader.load(SystemActorType.SignatureManager)
          val signatureInsertFuture = signatureManager ? kernelMessage

          // TODO: Handle error case for mapTo and non-present onFailure
          signatureInsertFuture.mapTo[KernelMessage] onSuccess {
            case message => relay(message)
          }
        } else {
          relay(kernelMessage)
        }
  }
}
