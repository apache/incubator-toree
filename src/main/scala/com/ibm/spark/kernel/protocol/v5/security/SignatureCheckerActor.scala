package com.ibm.spark.kernel.protocol.v5.security

import akka.actor.Actor
import com.ibm.spark.kernel.protocol.v5.KernelMessage
import com.ibm.spark.security.{HmacAlgorithm, Hmac}
import com.ibm.spark.utils.LogLike
import play.api.libs.json.Json

/**
 * Verifies whether or not a kernel message has a valid signature.
 * @param hmac The HMAC to use for signature validation
 */
class SignatureCheckerActor(
  private val hmac: Hmac
) extends Actor with LogLike {
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
