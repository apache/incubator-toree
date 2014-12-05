package com.ibm.spark.kernel.protocol.v5.security

import akka.actor.Actor
import com.ibm.spark.security.Hmac
import com.ibm.spark.utils.LogLike

/**
 * Verifies whether or not a kernel message has a valid signature.
 * @param hmac The HMAC to use for signature validation
 */
class SignatureCheckerActor(
  private val hmac: Hmac
) extends Actor with LogLike {
  override def receive: Receive = {
    case (signature: String, blob: Seq[_]) =>
      val stringBlob: Seq[String] = blob.map(_.toString)
      val hmacString = hmac(stringBlob: _*)
      val isValidSignature = hmacString == signature
      logger.trace(s"Signature ${signature} validity checked against " +
        s"hmac ${hmacString} with outcome ${isValidSignature}")
      sender ! isValidSignature
  }
}
