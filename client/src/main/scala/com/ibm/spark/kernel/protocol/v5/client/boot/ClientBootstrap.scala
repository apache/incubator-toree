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

package com.ibm.spark.kernel.protocol.v5.client.boot

import akka.actor.{ActorRef, ActorSystem, Props}
import com.ibm.spark.comm.{CommRegistrar, CommStorage}
import com.ibm.spark.kernel.protocol.v5.MessageType.MessageType
import com.ibm.spark.kernel.protocol.v5.client.boot.layers._
import com.ibm.spark.kernel.protocol.v5.{SimpleActorLoader, _}
import com.ibm.spark.kernel.protocol.v5.client.SparkKernelClient
import com.ibm.spark.kernel.protocol.v5.client.handler.ExecuteHandler
import com.ibm.spark.kernel.protocol.v5.socket.{SocketConfig, _}
import com.ibm.spark.utils.LogLike
import com.typesafe.config.Config

class ClientBootstrap(config: Config) extends LogLike {
  this: SystemInitialization with HandlerInitialization =>

  // set up our actor system and configure the socket factory
  private val actorSystem = ActorSystem("spark-client-actor-system")
  private val actorLoader = SimpleActorLoader(actorSystem)
  private val socketFactory =
    new ClientSocketFactory(SocketConfig.fromConfig(config))

  // TODO: All clients share the same storage (for testing purposes), this needs
  //       to be updated in the future
  private var commStorage: CommStorage = _

  /**
   * @return an instance of a SparkKernelClient
   */
  def createClient: SparkKernelClient = {
    val commRegistrar = new CommRegistrar(commStorage)
    val client = new SparkKernelClient(
      actorLoader, actorSystem, commRegistrar)
    client
  }

  /**
   * Initializes all kernel systems.
   */
  def initialize(): Unit = {
    val (_,_,_,_,s) = initializeSystem(actorSystem, actorLoader, socketFactory)

    this.commStorage = s

    initializeHandlers(actorSystem, actorLoader)
  }

  initialize()
}