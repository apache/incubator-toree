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

package com.ibm.spark.boot.layer

import akka.actor.{ActorRef, Props, ActorSystem}
import com.ibm.spark.kernel.protocol.v5.dispatch.StatusDispatch
import com.ibm.spark.kernel.protocol.v5.security.SignatureManagerActor
import com.ibm.spark.kernel.protocol.v5.socket._
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.{CommClose, CommMsg, CommOpen}
import com.ibm.spark.kernel.protocol.v5.relay.KernelMessageRelay
import com.ibm.spark.utils.LogLike
import com.typesafe.config.Config
import play.api.libs.json.Json

/**
 * Represents the raw initialization needed to send a "starting" message.
 */
trait BareInitialization {
  /**
   * Initializes and registers all objects needed to get the kernel to send a
   * "starting" message.
   *
   * @param config The config used for initialization
   * @param actorSystemName The name to use for the actor system
   */
  def initializeBare(config: Config, actorSystemName: String):
    (ActorSystem, ActorLoader, ActorRef, ActorRef)
}

/**
 * Represents the standard implementation of BareInitialization.
 */
trait StandardBareInitialization extends BareInitialization { this: LogLike =>
  /**
   * Initializes and registers all objects needed to get the kernel to send a
   * "starting" message.
   *
   * @param config The config used for initialization
   * @param actorSystemName The name to use for the actor system
   */
  def initializeBare(config: Config, actorSystemName: String) = {
    val (actorSystem, actorLoader, kernelMessageRelayActor, _, statusDispatch) =
      initializeBareComponents(config, actorSystemName)
    createSockets(config, actorSystem, actorLoader)

    (actorSystem, actorLoader, kernelMessageRelayActor, statusDispatch)
  }

  /**
   * Does minimal setup in order to send the "starting" status message over
   * the IOPub socket
   */
  private def initializeBareComponents(
    config: Config, actorSystemName: String
  ) = {
    logger.info("Initializing internal actor system")
    val actorSystem = ActorSystem(actorSystemName)

    logger.debug("Creating Simple Actor Loader")
    val actorLoader = SimpleActorLoader(actorSystem)

    logger.debug("Creating kernel message relay actor")
    val kernelMessageRelayActor = actorSystem.actorOf(
      Props(
        classOf[KernelMessageRelay], actorLoader, true,
        Map(
          CommOpen.toTypeString -> MessageType.Incoming.CommOpen.toString,
          CommMsg.toTypeString -> MessageType.Incoming.CommMsg.toString,
          CommClose.toTypeString -> MessageType.Incoming.CommClose.toString
        ),
        Map(
          CommOpen.toTypeString -> MessageType.Outgoing.CommOpen.toString,
          CommMsg.toTypeString -> MessageType.Outgoing.CommMsg.toString,
          CommClose.toTypeString -> MessageType.Outgoing.CommClose.toString
        )
      ),
      name = SystemActorType.KernelMessageRelay.toString
    )

    logger.debug("Creating signature manager actor")
    val sigKey = config.getString("key")
    val sigScheme = config.getString("signature_scheme")
    logger.debug("Key = " + sigKey)
    logger.debug("Scheme = " + sigScheme)
    val signatureManagerActor = actorSystem.actorOf(
      Props(
        classOf[SignatureManagerActor], sigKey, sigScheme.replace("-", "")
      ),
      name = SystemActorType.SignatureManager.toString
    )

    logger.debug("Creating status dispatch actor")
    val statusDispatch = actorSystem.actorOf(
      Props(classOf[StatusDispatch], actorLoader),
      name = SystemActorType.StatusDispatch.toString
    )

    (actorSystem, actorLoader, kernelMessageRelayActor, signatureManagerActor,
      statusDispatch)
  }

  private def createSockets(
    config: Config, actorSystem: ActorSystem, actorLoader: ActorLoader
  ) = {
    logger.debug("Creating sockets")

    val socketConfig: SocketConfig = SocketConfig.fromConfig(config)
    logger.info("Connection Profile: "
      + Json.prettyPrint(Json.toJson(socketConfig)))

    logger.debug("Constructing ServerSocketFactory")
    val socketFactory = new ServerSocketFactory(socketConfig)

    logger.debug("Initializing Heartbeat on port " +
      socketConfig.hb_port)
    val heartbeatActor = actorSystem.actorOf(
      Props(classOf[Heartbeat], socketFactory),
      name = SocketType.Heartbeat.toString
    )

    logger.debug("Initializing Shell on port " +
      socketConfig.shell_port)
    val shellActor = actorSystem.actorOf(
      Props(classOf[Shell], socketFactory, actorLoader),
      name = SocketType.Shell.toString
    )

    logger.debug("Initializing IOPub on port " +
      socketConfig.iopub_port)
    val ioPubActor = actorSystem.actorOf(
      Props(classOf[IOPub], socketFactory),
      name = SocketType.IOPub.toString
    )

    (heartbeatActor, shellActor, ioPubActor)
  }
}
