/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
