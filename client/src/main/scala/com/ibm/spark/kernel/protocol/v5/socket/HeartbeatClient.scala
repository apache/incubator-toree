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

package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.{ActorRef, Actor}
import akka.util.{ByteString, Timeout}
import akka.zeromq.ZMQMessage
import akka.pattern.ask
import com.ibm.spark.utils.LogLike
import com.ibm.spark.kernel.protocol.v5.UUID
import scala.collection.concurrent.{Map, TrieMap}
import scala.concurrent.duration._

object HeartbeatMessage {}
/**
 * The client endpoint for heartbeat messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class HeartbeatClient(socketFactory : ClientSocketFactory) extends Actor with LogLike {
  logger.debug("Created new Heartbeat Client actor")
  implicit val timeout = Timeout(1.minute)

  val futureMap: Map[UUID, ActorRef] = TrieMap[UUID, ActorRef]()
  val socket = socketFactory.HeartbeatClient(context.system, self)

  override def receive: Receive = {
    // from Heartbeat
    case message: ZMQMessage =>
      val id = message.frames.map((byteString: ByteString) => new String(byteString.toArray)).mkString("\n")
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