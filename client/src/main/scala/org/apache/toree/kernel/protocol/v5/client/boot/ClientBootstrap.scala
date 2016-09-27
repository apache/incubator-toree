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

package org.apache.toree.kernel.protocol.v5.client.boot

import akka.actor.ActorSystem
import org.apache.toree.comm.{CommRegistrar, CommStorage}
import org.apache.toree.kernel.protocol.v5.client.boot.layers._
import org.apache.toree.kernel.protocol.v5.client.socket.{SocketConfig, SocketFactory}
import org.apache.toree.kernel.protocol.v5.client.{SimpleActorLoader, SparkKernelClient}
import org.apache.toree.utils.LogLike
import com.typesafe.config.Config
import org.zeromq.ZMQ

object ClientBootstrap {
  /**
   * Generates a new unique name for a client actor system.
   *
   * @return The unique name as a string
   */
  def newActorSystemName(): String =
    "spark-client-actor-system-" + java.util.UUID.randomUUID().toString
}

class ClientBootstrap(config: Config) extends LogLike {
  this: SystemInitialization with HandlerInitialization =>

  /**
   * Creates a new Spark Kernel client instance.
   *
   * @return The new client instance
   */
  def createClient(
    actorSystemName: String = ClientBootstrap.newActorSystemName()
  ): SparkKernelClient = {
    logger.trace(s"Creating new kernel client actor system, '$actorSystemName'")
    val actorSystem = ActorSystem(actorSystemName)

    logger.trace(s"Creating actor loader for actor system, '$actorSystemName'")
    val actorLoader = SimpleActorLoader(actorSystem)

    logger.trace(s"Creating socket factory for actor system, '$actorSystemName")
    val socketFactory = new SocketFactory(SocketConfig.fromConfig(config))

    logger.trace(s"Initializing underlying system for, '$actorSystemName'")
    val (_, _, _, _, commRegistrar, _) =
      initializeSystem(config, actorSystem, actorLoader, socketFactory)

    logger.trace(s"Initializing handlers for, '$actorSystemName'")
    initializeHandlers(actorSystem, actorLoader)

    logger.trace(s"ZeroMQ version: ${ZMQ.getVersionString}")

    new SparkKernelClient(actorLoader, actorSystem, commRegistrar)
  }
}