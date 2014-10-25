package com.ibm.spark.kernel.protocol.v5.security

import akka.actor.Actor
import com.ibm.spark.kernel.protocol.v5.KernelMessage
import com.ibm.spark.security.Hmac
import com.ibm.spark.utils.LogLike
import play.api.libs.json.Json

/**
 * Constructs a signature from any kernel message received.
 * @param hmac The HMAC to use for signature construction
 */
class SignatureProducerActor(
  private val hmac: Hmac
) extends Actor with LogLike {
  override def receive: Receive = {
    case message: KernelMessage =>
      val signature = hmac(
        Json.stringify(Json.toJson(message.header)),
        Json.stringify(Json.toJson(message.parentHeader)),
        Json.stringify(Json.toJson(message.metadata)),
        message.contentString
      )
      sender ! signature
  }
}
