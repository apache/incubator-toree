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

package org.apache.toree.communication.security

import akka.actor.Actor
import org.apache.toree.communication.utils.OrderedSupport
import org.apache.toree.utils.LogLike

/**
 * Verifies whether or not a kernel message has a valid signature.
 * @param hmac The HMAC to use for signature validation
 */
class SignatureCheckerActor(
  private val hmac: Hmac
) extends Actor with LogLike with OrderedSupport {
  override def receive: Receive = {
    case (signature: String, blob: Seq[_]) => withProcessing {
      val stringBlob: Seq[String] = blob.map(_.toString)
      val hmacString = hmac(stringBlob: _*)
      val isValidSignature = hmacString == signature
      logger.trace(s"Signature ${signature} validity checked against " +
        s"hmac ${hmacString} with outcome ${isValidSignature}")
      sender ! isValidSignature
    }
  }

  /**
   * Defines the types that will be stashed by [[waiting]]
   * while the Actor is in processing state.
   * @return
   */
  override def orderedTypes(): Seq[Class[_]] = Seq(classOf[(String, Seq[_])])
}
