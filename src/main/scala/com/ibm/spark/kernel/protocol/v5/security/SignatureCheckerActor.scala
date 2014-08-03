package com.ibm.spark.kernel.protocol.v5.security

import akka.actor.{ActorLogging, Actor}
import com.ibm.spark.kernel.protocol.v5.KernelMessage
import com.ibm.spark.security.{HmacAlgorithm, Hmac}
import play.api.libs.json.Json

/**
 * Verifies whether or not a kernel message has a valid signature.
 * @param hmac The HMAC to use for signature validation
 */
class SignatureCheckerActor(
  private val hmac: Hmac
) extends Actor with ActorLogging {
  override def receive: Receive = {
    case message: KernelMessage =>
      val isValidSignature = hmac(
        Json.toJson(message.header).toString,
        Json.toJson(message.parentHeader).toString,
        Json.toJson(message.metadata).toString,
        message.contentString
      ) == message.signature
      sender ! isValidSignature
  }
}
