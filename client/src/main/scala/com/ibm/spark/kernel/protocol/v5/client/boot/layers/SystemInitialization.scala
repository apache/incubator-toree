/*
 * Copyright 2014 IBM Corp.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.ibm.spark.kernel.protocol.v5.client.boot.layers

import akka.actor.{Props, ActorRef, ActorSystem}
import com.ibm.spark.comm.{CommRegistrar, CommStorage}
import com.ibm.spark.kernel.protocol.v5.SocketType
import com.ibm.spark.kernel.protocol.v5.client.ActorLoader
import com.ibm.spark.kernel.protocol.v5.client.socket._

/**
 * Represents the system-related initialization such as socket actors.
 */
trait SystemInitialization {
  /**
   * Initializes the system-related client objects.
   *
   * @param actorSystem The actor system used by the client
   * @param actorLoader The actor loader used by the client
   * @param socketFactory The socket factory used by the client
   *
   * @return The heartbeat, stdin, shell, and IOPub client actors and the Comm
   *         registrar and storage used for Comm callbacks
   */
  def initializeSystem(
    actorSystem: ActorSystem, actorLoader: ActorLoader,
    socketFactory: SocketFactory
  ): (ActorRef, ActorRef, ActorRef, ActorRef, CommRegistrar, CommStorage)
}

/**
 * Represents the standard implementation of SystemInitialization.
 */
trait StandardSystemInitialization extends SystemInitialization {
  /**
   * Initializes the system-related client objects.
   *
   * @param actorSystem The actor system used by the client
   * @param actorLoader The actor loader used by the client
   * @param socketFactory The socket factory used by the client
   *
   * @return The heartbeat, shell, and IOPub client actors
   */
  override def initializeSystem(
    actorSystem: ActorSystem, actorLoader: ActorLoader,
    socketFactory: SocketFactory
  ): (ActorRef, ActorRef, ActorRef, ActorRef, CommRegistrar, CommStorage) = {
    val commStorage = new CommStorage()
    val commRegistrar = new CommRegistrar(commStorage)

    val (heartbeat, stdin, shell, ioPub) = initializeSystemActors(
      actorSystem = actorSystem,
      actorLoader = actorLoader,
      socketFactory = socketFactory,
      commRegistrar = commRegistrar,
      commStorage = commStorage
    )

    (heartbeat, stdin, shell, ioPub, commRegistrar, commStorage)
  }

  private def initializeSystemActors(
    actorSystem: ActorSystem, actorLoader: ActorLoader,
    socketFactory: SocketFactory, commRegistrar: CommRegistrar,
    commStorage: CommStorage
  ) = {
    val heartbeatClient = actorSystem.actorOf(
      Props(classOf[HeartbeatClient], socketFactory),
      name = SocketType.HeartbeatClient.toString
    )

    val stdinClient = actorSystem.actorOf(
      Props(classOf[StdinClient], socketFactory),
      name = SocketType.StdInClient.toString
    )

    val shellClient = actorSystem.actorOf(
      Props(classOf[ShellClient], socketFactory),
      name = SocketType.ShellClient.toString
    )

    val ioPubClient = actorSystem.actorOf(
      Props(classOf[IOPubClient], socketFactory, actorLoader,
        commRegistrar, commStorage),
      name = SocketType.IOPubClient.toString
    )

    (heartbeatClient, stdinClient, shellClient, ioPubClient)
  }
}
