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
import org.apache.toree.kernel.protocol.v5.KernelMessage
import org.apache.toree.utils.LogLike
import play.api.libs.json.Json

/**
 * Constructs a signature from any kernel message received.
 * @param hmac The HMAC to use for signature construction
 */
class SignatureProducerActor(
  private val hmac: Hmac
) extends Actor with LogLike with OrderedSupport {
  override def receive: Receive = {
    case message: KernelMessage => withProcessing {
      val signature = hmac(
        Json.stringify(Json.toJson(message.header)),
        Json.stringify(Json.toJson(message.parentHeader)),
        Json.stringify(Json.toJson(message.metadata)),
        message.contentString
      )
      sender ! signature
    }
  }

  /**
   * Defines the types that will be stashed by [[waiting]]
   * while the Actor is in processing state.
   * @return
   */
  override def orderedTypes(): Seq[Class[_]] = Seq(classOf[KernelMessage])
}
