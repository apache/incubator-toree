package com.ibm.spark.kernel.protocol.v5.security

import akka.actor.{ActorLogging, Actor}
import com.ibm.spark.kernel.protocol.v5.KernelMessage
import com.ibm.spark.security.{HmacAlgorithm, Hmac}
import play.libs.Json

/**
 * Verifies whether or not a kernel message has a valid signature.
 * @param key The key to use for signature validation
 * @param scheme The scheme to use for signature validation
 */
class SignatureCheckerActor(
  key: String, scheme: String
) extends Actor with ActorLogging {
  private val hmac = Hmac(key, HmacAlgorithm(scheme))

  def this(key: String) = this(key, HmacAlgorithm.SHA256.toString)

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
