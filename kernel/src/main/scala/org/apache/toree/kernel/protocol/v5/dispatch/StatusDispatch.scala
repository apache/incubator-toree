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

package org.apache.toree.kernel.protocol.v5.dispatch

import akka.actor.Actor
import org.apache.toree.kernel.protocol.v5.KernelStatusType.KernelStatusType
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content.KernelStatus
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.utils.LogLike
import play.api.libs.json.Json

class StatusDispatch(actorLoader: ActorLoader) extends Actor with LogLike {
  private def sendStatusMessage(kernelStatus: KernelStatusType, parentHeader: Header) {
    //  Create the status message and send it to the relay
    val km : KernelMessage = KMBuilder()
      .withIds(Seq(MessageType.Outgoing.Status.toString.getBytes))
      .withSignature("")
      .withHeader(MessageType.Outgoing.Status)
      .withParentHeader(parentHeader)
      .withContentString(KernelStatus(kernelStatus.toString)).build

    actorLoader.load(SystemActorType.KernelMessageRelay) ! km
  }

  override def receive: Receive = {
    case (status: KernelStatusType, null) =>
      //  TODO Determine if this should be null or an empty parent header
      sendStatusMessage(status, null)

    case (status: KernelStatusType, parentHeader: Header) =>
      sendStatusMessage( status, parentHeader )

    case status: KernelStatusType =>
      sendStatusMessage(status , HeaderBuilder.empty)
  }
}
