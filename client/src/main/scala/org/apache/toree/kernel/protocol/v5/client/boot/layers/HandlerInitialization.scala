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

package org.apache.toree.kernel.protocol.v5.client.boot.layers

import akka.actor.{ActorSystem, Props}
import org.apache.toree.kernel.protocol.v5.client.ActorLoader
import org.apache.toree.kernel.protocol.v5.MessageType
import org.apache.toree.kernel.protocol.v5.MessageType.MessageType
import org.apache.toree.kernel.protocol.v5.client.handler.ExecuteHandler
import org.apache.toree.utils.LogLike

/**
 * Represents the event handler initialization such as message handlers.
 */
trait HandlerInitialization {
  /**
   * Initializes event handlers.
   *
   * @param actorSystem The actor system used by the client
   * @param actorLoader The actor loader used by the client
   */
  def initializeHandlers(
    actorSystem: ActorSystem, actorLoader: ActorLoader
  ): Unit
}

/**
 * Represents the standard implementation of HandlerInitialization.
 */
trait StandardHandlerInitialization extends HandlerInitialization {
  this: LogLike =>

  /**
   * Initializes event handlers.
   *
   * @param actorSystem The actor system used by the client
   * @param actorLoader The actor loader used by the client
   */
  override def initializeHandlers(
    actorSystem: ActorSystem, actorLoader: ActorLoader
  ): Unit = initializeMessageHandlers(actorSystem, actorLoader)

  private def initializeRequestHandler[T](
    actorSystem: ActorSystem, actorLoader: ActorLoader,
    clazz: Class[T], messageType: MessageType
  ) = {
    logger.info("Creating %s handler".format(messageType.toString))
    actorSystem.actorOf(Props(clazz, actorLoader), name = messageType.toString)
  }

  private def initializeMessageHandlers(
    actorSystem: ActorSystem, actorLoader: ActorLoader
  ): Unit = {
    initializeRequestHandler(
      actorSystem,
      actorLoader,
      classOf[ExecuteHandler],
      MessageType.Incoming.ExecuteRequest
    )
  }
}