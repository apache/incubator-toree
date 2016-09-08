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

package org.apache.toree.kernel.protocol.v5.handler

import org.apache.toree.comm.CommStorage
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content.CommInfoReply
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.utils.MessageLogSupport
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, future}

/**
 * Receives a CommInfoRequest KernelMessage and returns a CommInfoReply
 * KernelMessage.
 *
 * @param actorLoader The actor loader to use for actor communication
 * @param commStorage The Comm storage used for msg callbacks
 */
class CommInfoRequestHandler(
                              actorLoader: ActorLoader,
                              commStorage: CommStorage)
  extends BaseHandler(actorLoader) with MessageLogSupport
{

  def buildCommMap(targetName: String) = {
    commStorage.getCommIdsFromTarget(targetName) match {
      case Some(commVector) => {
        commVector.map(x => Map(x -> Map("target_name" -> targetName))).flatten.toMap
      }
      case _ => {
        Map()
      }
    }
  }

  override def process(kernelMessage: KernelMessage): Future[_] = Future {
    logKernelMessageAction("Initiating CommInfo request for", kernelMessage)

    val commMap = (Json.parse(kernelMessage.contentString) \ "target_name").asOpt[String] match {
      case Some(targetName) => {
        buildCommMap(targetName)
      }
      case None => {
        //target_name is missing from the kernel message so return all comms over every target
        commStorage.getTargets().map(buildCommMap(_)).reduce(_ ++ _)
      }
    }
    val commInfoReply = CommInfoReply(commMap.asInstanceOf[Map[String, Map[String, String]]])

    val kernelInfo = SparkKernelInfo

    val replyHeader = Header(
      java.util.UUID.randomUUID.toString,
      "",
      java.util.UUID.randomUUID.toString,
      CommInfoReply.toTypeString,
      kernelInfo.protocolVersion)

    val kernelResponseMessage = KMBuilder()
      .withIds(kernelMessage.ids)
      .withSignature("")
      .withHeader(replyHeader)
      .withParent(kernelMessage)
      .withContentString(commInfoReply).build

    actorLoader.load(SystemActorType.KernelMessageRelay) ! kernelResponseMessage
  }

}

