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

package com.ibm.spark.kernel.protocol.v5.client

import akka.actor.{ActorRef, ActorSystem, Props}
import com.ibm.spark.kernel.protocol.v5.MessageType.MessageType
import com.ibm.spark.kernel.protocol.v5.SimpleActorLoader
import com.ibm.spark.kernel.protocol.v5.client.handler.ExecuteHandler
import com.ibm.spark.kernel.protocol.v5.socket._
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.socket.SocketConfig
import com.ibm.spark.utils.LogLike
import com.typesafe.config.Config

class SparkKernelClientBootstrap(config: Config) extends LogLike {

  // set up our actor system and configure the socket factory
  private val actorSystem = ActorSystem("spark-client-actor-system")
  private val actorLoader = SimpleActorLoader(actorSystem)
  private val socketFactory = new ClientSocketFactory(SocketConfig.fromConfig(config))

  private var heartbeatClientActor: Option[ActorRef] = None

  /**
   * @return an instance of a SparkKernelClient
   */
  def createClient: SparkKernelClient = {
    val client = new SparkKernelClient(actorLoader, actorSystem)
    client
  }

  /**
   * Initializes all kernel systems.
   */
  def initialize(): Unit = {
    initializeSystemActors()
    initializeMessageHandlers()
  }

  private def initializeSystemActors(): Unit = {
    heartbeatClientActor = Option(actorSystem.actorOf(Props(classOf[HeartbeatClient], socketFactory),
      name = SocketType.HeartbeatClient.toString))

    actorSystem.actorOf(Props(classOf[ShellClient], socketFactory),
      name = SocketType.ShellClient.toString)

    actorSystem.actorOf(Props(classOf[IOPubClient], socketFactory),
      name = SocketType.IOPubClient.toString)
  }

  private def initializeRequestHandler[T](clazz: Class[T], messageType: MessageType) {
    logger.info("Creating %s handler".format(messageType.toString))
    actorSystem.actorOf(Props(clazz, actorLoader), name = messageType.toString)
  }

  private def initializeMessageHandlers(): Unit = {
    initializeRequestHandler(classOf[ExecuteHandler], MessageType.ExecuteRequest)
  }

  initialize()
}