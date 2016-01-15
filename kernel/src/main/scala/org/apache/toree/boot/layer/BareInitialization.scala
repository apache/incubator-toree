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

package org.apache.toree.boot.layer

import akka.actor.{ActorRef, Props, ActorSystem}
import org.apache.toree.kernel.protocol.v5.dispatch.StatusDispatch
import org.apache.toree.kernel.protocol.v5.handler.{GenericSocketMessageHandler, ShutdownHandler}
import org.apache.toree.kernel.protocol.v5.kernel.{SimpleActorLoader, ActorLoader}
import org.apache.toree.communication.security.{SecurityActorType, SignatureManagerActor}
import org.apache.toree.kernel.protocol.v5.kernel.socket._
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content.{CommClose, CommMsg, CommOpen}
import org.apache.toree.kernel.protocol.v5.relay.KernelMessageRelay
import org.apache.toree.utils.LogLike
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
    val actorSystem = createActorSystem(actorSystemName)
    val actorLoader = createActorLoader(actorSystem)
    val (kernelMessageRelayActor, _, statusDispatch, _, _) =
      initializeCoreActors(config, actorSystem, actorLoader)
    createSockets(config, actorSystem, actorLoader)

    (actorSystem, actorLoader, kernelMessageRelayActor, statusDispatch)
  }

  protected def createActorSystem(actorSystemName: String): ActorSystem = {
    logger.info("Initializing internal actor system")
    ActorSystem(actorSystemName)
  }

  protected def createActorLoader(actorSystem: ActorSystem): ActorLoader = {
    logger.debug("Creating Simple Actor Loader")
    SimpleActorLoader(actorSystem)
  }

  /**
   * Does minimal setup in order to send the "starting" status message over
   * the IOPub socket
   */
  protected def initializeCoreActors(
    config: Config, actorSystem: ActorSystem, actorLoader: ActorLoader
  ) = {
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
      name = SecurityActorType.SignatureManager.toString
    )

    logger.debug("Creating status dispatch actor")
    val statusDispatch = actorSystem.actorOf(
      Props(classOf[StatusDispatch], actorLoader),
      name = SystemActorType.StatusDispatch.toString
    )

    logger.debug("Creating shutdown handler and sender actors")
    val shutdownHandler = actorSystem.actorOf(
      Props(classOf[ShutdownHandler], actorLoader),
      name = MessageType.Incoming.ShutdownRequest.toString
    )
    val shutdownSender = actorSystem.actorOf(
      Props(classOf[GenericSocketMessageHandler], actorLoader, SocketType.Control),
      name = MessageType.Outgoing.ShutdownReply.toString
    )

    (kernelMessageRelayActor, signatureManagerActor, statusDispatch, shutdownHandler, shutdownSender)
  }

  protected def createSockets(
    config: Config, actorSystem: ActorSystem, actorLoader: ActorLoader
  ): Unit = {
    logger.debug("Creating sockets")

    val socketConfig: SocketConfig = SocketConfig.fromConfig(config)
    logger.info("Connection Profile: "
      + Json.prettyPrint(Json.toJson(socketConfig)))

    logger.debug("Constructing ServerSocketFactory")
    val socketFactory = new SocketFactory(socketConfig)

    logger.debug("Initializing Heartbeat on port " +
      socketConfig.hb_port)
    val heartbeatActor = actorSystem.actorOf(
      Props(classOf[Heartbeat], socketFactory),
      name = SocketType.Heartbeat.toString
    )

    logger.debug("Initializing Stdin on port " +
      socketConfig.stdin_port)
    val stdinActor = actorSystem.actorOf(
      Props(classOf[Stdin], socketFactory, actorLoader),
      name = SocketType.StdIn.toString
    )

    logger.debug("Initializing Shell on port " +
      socketConfig.shell_port)
    val shellActor = actorSystem.actorOf(
      Props(classOf[Shell], socketFactory, actorLoader),
      name = SocketType.Shell.toString
    )

    logger.debug("Initializing Control on port " +
      socketConfig.control_port)
    val controlActor = actorSystem.actorOf(
      Props(classOf[Control], socketFactory, actorLoader),
      name = SocketType.Control.toString
    )

    logger.debug("Initializing IOPub on port " +
      socketConfig.iopub_port)
    val ioPubActor = actorSystem.actorOf(
      Props(classOf[IOPub], socketFactory),
      name = SocketType.IOPub.toString
    )
  }
}
