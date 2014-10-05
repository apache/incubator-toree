package com.ibm.spark.kernel.protocol.v5.relay


import akka.actor.Actor
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
) extends Actor with LogLike {
  // NOTE: Required to provide the execution context for futures with akka
  import context._

  // NOTE: Required for ask (?) to function... maybe can define elsewhere?
  implicit val timeout = Timeout(5.seconds)

  // Flag indicating if can receive messages (or add them to buffer)
  var isReady = false
  val MaxMessageBufferSize = 10
  val messageBuffer =
    new collection.mutable.Queue[(Seq[_], KernelMessage)]()

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

  private def isStatusMessage(kernelMessage: KernelMessage) =
    kernelMessage.header.msg_type == MessageType.Status.toString

  /**
   * This actor will receive and handle two types; ZMQMessage and KernelMessage. These messages
   * will be forwarded to their actors which are responsible for them.
   */
  override def receive = {
    // TODO: How to restore this when the actor dies?
    case ready: Boolean =>
      isReady = ready
      logger.info("Relaying " + messageBuffer.size + " messages!")
      messageBuffer.dequeueAll(_ => true).foreach(self ! _)
      logger.info("Relay is now fully ready to receive messages!")

    // Add incoming messages (when not ready) to buffer to be processed
    case (zmqStrings: Seq[_], kernelMessage: KernelMessage) if !isReady =>
      if (messageBuffer.size < MaxMessageBufferSize)
        messageBuffer.enqueue((zmqStrings, kernelMessage))
      else
        logger.warn("Message buffer is full! Discarding message of type: "
          + kernelMessage.header.msg_type)

    // Assuming these messages are incoming messages
    case (zmqStrings: Seq[_], kernelMessage: KernelMessage) if isReady =>
      //  Send the busy message
      actorLoader.load(SystemActorType.StatusDispatch) !
        Tuple2(KernelStatusType.Busy, kernelMessage.header)

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
      // TODO: Investigate cleaner, less hard-coded solution
      // Send idle message (unless our outgoing status is the message type)
      if (!isStatusMessage(kernelMessage)) {
        actorLoader.load(SystemActorType.StatusDispatch) !
          Tuple2(KernelStatusType.Idle, kernelMessage.parentHeader)
      }

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
