package com.ibm.spark.kernel.protocol.v5.relay


import akka.actor.{Stash, Actor}
import akka.pattern.ask
import akka.util.{ByteString, Timeout}
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.utils.LogLike

import scala.concurrent.duration._
import scala.Tuple2
import com.ibm.spark.kernel.protocol.v5.KernelMessage
import com.ibm.spark.kernel.protocol.v5.MessageType.MessageType
import com.ibm.spark.kernel.protocol.v5.MessageType
import akka.zeromq.ZMQMessage
import java.nio.charset.Charset

/**
 * This class is meant to be a relay for send KernelMessages through kernel system.
 * @param actorLoader The ActorLoader used by this class for finding actors for relaying messages
 */
case class KernelMessageRelay(
  actorLoader: ActorLoader,
  useSignatureManager: Boolean
) extends Actor with LogLike with Stash {
  // NOTE: Required to provide the execution context for futures with akka
  import context._

  // NOTE: Required for ask (?) to function... maybe can define elsewhere?
  implicit val timeout = Timeout(5.seconds)

  // Flag indicating if can receive messages (or add them to buffer)
  var isReady = false

  def this(actorLoader: ActorLoader) =
    this(actorLoader, true)

  /**
   * Relays a KernelMessage to a specific actor to handle that message
   * @param kernelMessage The message to relay
   */
  private def relay(kernelMessage: KernelMessage) = {
    val messageType: MessageType = MessageType.withName(kernelMessage.header.msg_type)
    logger.info("Relaying message of type " + kernelMessage.header.msg_type )
    actorLoader.load(messageType) ! kernelMessage
  }


  /**
   * This actor will receive and handle two types; ZMQMessage and KernelMessage. These messages
   * will be forwarded to their actors which are responsible for them.
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
    case (zmqStrings: Seq[_], kernelMessage: KernelMessage) if !isReady =>
      logger.info("Not ready for messages! Stashing until ready!")
      stash()

    // Assuming these messages are incoming messages
    case (zmqStrings: Seq[_], kernelMessage: KernelMessage) if isReady =>
      if (useSignatureManager) {
        val signatureManager = actorLoader.load(SystemActorType.SignatureManager)
        val signatureVerificationFuture = signatureManager ? ((
          kernelMessage.signature, zmqStrings
        ))

        // TODO: Handle error case for mapTo and non-present onFailure
        signatureVerificationFuture.mapTo[Boolean] onSuccess {
          // Verification successful, so continue relay
          case true => relay(kernelMessage)

          // TODO: Figure out what the failure message structure should be!
          // Verification failed, so report back a failure
          case false =>
            logger.error("Invalid signature received from message!")
        }
      } else {
        relay(kernelMessage)
      }

    // Assuming all kernel messages without zmq strings are outgoing
    case kernelMessage: KernelMessage =>

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
