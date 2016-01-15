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

package org.apache.toree.kernel.protocol.v5.client.socket

import akka.actor.{ActorRef, Actor}
import akka.util.{ByteString, Timeout}
import org.apache.toree.communication.ZMQMessage
import akka.pattern.ask
import org.apache.toree.kernel.protocol.v5.client.ActorLoader
import org.apache.toree.utils.LogLike
import org.apache.toree.kernel.protocol.v5.UUID
import scala.collection.concurrent.{Map, TrieMap}
import scala.concurrent.duration._

object HeartbeatMessage {}
/**
 * The client endpoint for heartbeat messages specified in the IPython Kernel
 * Spec.
 *
 * @param socketFactory A factory to create the ZeroMQ socket connection
 * @param actorLoader The loader used to retrieve actors
 * @param signatureEnabled Whether or not to check and provide signatures
 */
class HeartbeatClient(
  socketFactory : SocketFactory,
  actorLoader: ActorLoader,
  signatureEnabled: Boolean
) extends Actor with LogLike {
  logger.debug("Created new Heartbeat Client actor")
  implicit val timeout = Timeout(1.minute)

  val futureMap: Map[UUID, ActorRef] = TrieMap[UUID, ActorRef]()
  val socket = socketFactory.HeartbeatClient(context.system, self)

  override def receive: Receive = {
    // from Heartbeat
    case message: ZMQMessage =>
      val id = message.frames.map((byteString: ByteString) =>
        new String(byteString.toArray)).mkString("\n")
      logger.info(s"Heartbeat client receive:$id")
      futureMap(id) ! true
      futureMap.remove(id)

    // from SparkKernelClient
    case HeartbeatMessage =>
      import scala.concurrent.ExecutionContext.Implicits.global
      val id = java.util.UUID.randomUUID().toString
      futureMap += (id -> sender)
      logger.info(s"Heartbeat client send: $id")
      val future = socket ? ZMQMessage(ByteString(id.getBytes))
      future.onComplete {
        // future always times out because server "tells" response {
        case(_) => futureMap.remove(id)
      }
  }
}