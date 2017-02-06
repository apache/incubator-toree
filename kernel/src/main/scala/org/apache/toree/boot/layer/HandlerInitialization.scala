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

import akka.actor.{ActorRef, ActorSystem, Props}
import org.apache.toree.comm.{CommRegistrar, CommStorage}
import org.apache.toree.interpreter.Interpreter
import org.apache.toree.kernel.api.Kernel
import org.apache.toree.kernel.protocol.v5.MessageType.MessageType
import org.apache.toree.kernel.protocol.v5.SocketType.SocketType
import org.apache.toree.kernel.protocol.v5.handler._
import org.apache.toree.kernel.protocol.v5.interpreter.InterpreterActor
import org.apache.toree.kernel.protocol.v5.interpreter.tasks.InterpreterTaskFactory
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.kernel.protocol.v5.magic.MagicParser
import org.apache.toree.kernel.protocol.v5.relay.ExecuteRequestRelay
import org.apache.toree.kernel.protocol.v5.{MessageType, SocketType, SystemActorType, LanguageInfo}
import org.apache.toree.magic.MagicManager
import org.apache.toree.plugins.PluginManager
import org.apache.toree.utils.LogLike

/**
 * Represents the Akka handler initialization. All actors (not needed in bare
 * initialization) should be constructed here.
 */
trait HandlerInitialization {
  /**
   * Initializes and registers all handlers.
   *
   * @param actorSystem The actor system needed for registration
   * @param actorLoader The actor loader needed for registration
   * @param kernel The kernel api needed for registration
   * @param interpreter The main interpreter needed for registration
   * @param magicManager The magic manager needed for registration
   * @param commRegistrar The comm registrar needed for registration
   * @param commStorage The comm storage needed for registration
   */
  def initializeHandlers(
    actorSystem: ActorSystem, actorLoader: ActorLoader,
    kernel: Kernel,
    interpreter: Interpreter, pluginManager: PluginManager,
    magicManager: MagicManager,   commRegistrar: CommRegistrar,
    commStorage: CommStorage,
    responseMap: collection.mutable.Map[String, ActorRef]
  ): Unit
}

/**
 * Represents the standard implementation of HandlerInitialization.
 */
trait StandardHandlerInitialization extends HandlerInitialization {
  this: LogLike =>

  /**
   * Initializes and registers all handlers.
   *
   * @param actorSystem The actor system needed for registration
   * @param actorLoader The actor loader needed for registration
   * @param kernel The kernel api needed for registration
   * @param interpreter The main interpreter needed for registration
   * @param pluginManager The plugin manager needed for registration
   * @param commRegistrar The comm registrar needed for registration
   * @param commStorage The comm storage needed for registration
   */
  def initializeHandlers(
    actorSystem: ActorSystem, actorLoader: ActorLoader,
    kernel: Kernel,
    interpreter: Interpreter, pluginManager: PluginManager,
    magicManager: MagicManager, commRegistrar: CommRegistrar,
    commStorage: CommStorage,
    responseMap: collection.mutable.Map[String, ActorRef]
  ): Unit = {
    initializeKernelHandlers(
      actorSystem, actorLoader, interpreter, kernel, commRegistrar, commStorage, responseMap
    )
    initializeSystemActors(
      actorSystem, actorLoader, interpreter, pluginManager, magicManager
    )
  }

  private def initializeSystemActors(
    actorSystem: ActorSystem, actorLoader: ActorLoader,
    interpreter: Interpreter, pluginManager: PluginManager,
    magicManager: MagicManager
  ): Unit = {
    logger.debug("Creating interpreter actor")
    val interpreterActor = actorSystem.actorOf(
      Props(classOf[InterpreterActor], new InterpreterTaskFactory(interpreter)),
      name = SystemActorType.Interpreter.toString
    )

    logger.debug("Creating execute request relay actor")
    val magicParser = new MagicParser(magicManager)
    val executeRequestRelayActor = actorSystem.actorOf(
      Props(classOf[ExecuteRequestRelay],
        actorLoader, pluginManager, magicParser
      ),
      name = SystemActorType.ExecuteRequestRelay.toString
    )
  }

  private def initializeKernelHandlers(
    actorSystem: ActorSystem, actorLoader: ActorLoader,
    interpreter: Interpreter, kernel: Kernel,
    commRegistrar: CommRegistrar, commStorage: CommStorage,
    responseMap: collection.mutable.Map[String, ActorRef]
  ): Unit = {
    def initializeRequestHandler[T](clazz: Class[T], messageType: MessageType, extraArguments: AnyRef*) = {
      logger.debug("Creating %s handler".format(messageType.toString))
      actorSystem.actorOf(
        Props(clazz, actorLoader +: extraArguments: _*),
        name = messageType.toString
      )
    }

    def initializeInputHandler[T](
      clazz: Class[T],
      messageType: MessageType
    ): Unit = {
      logger.debug("Creating %s handler".format(messageType.toString))
      actorSystem.actorOf(
        Props(clazz, actorLoader, responseMap),
        name = messageType.toString
      )
    }

    // TODO: Figure out how to pass variable number of arguments to actor
    def initializeCommHandler[T](clazz: Class[T], messageType: MessageType) = {
      logger.debug("Creating %s handler".format(messageType.toString))
      actorSystem.actorOf(
        Props(clazz, actorLoader, commRegistrar, commStorage),
        name = messageType.toString
      )
    }

    def initializeSocketHandler(socketType: SocketType, messageType: MessageType): Unit = {
      logger.debug("Creating %s to %s socket handler ".format(messageType.toString ,socketType.toString))
      actorSystem.actorOf(
        Props(classOf[GenericSocketMessageHandler], actorLoader, socketType),
        name = messageType.toString
      )
    }

    val langInfo = interpreter.languageInfo
    val internalInfo = LanguageInfo(
      name=langInfo.name,
      version=langInfo.version,
      file_extension=langInfo.fileExtension,
      pygments_lexer=langInfo.pygmentsLexer,
      mimetype=langInfo.mimeType,
      codemirror_mode=langInfo.codemirrorMode)

    //  These are the handlers for messages coming into the
    initializeRequestHandler(classOf[ExecuteRequestHandler],
      MessageType.Incoming.ExecuteRequest, kernel)
    initializeRequestHandler(classOf[KernelInfoRequestHandler],
      MessageType.Incoming.KernelInfoRequest, internalInfo)
    initializeRequestHandler(classOf[CommInfoRequestHandler],
      MessageType.Incoming.CommInfoRequest, commStorage)
    initializeRequestHandler(classOf[CodeCompleteHandler],
      MessageType.Incoming.CompleteRequest)
    initializeRequestHandler(classOf[IsCompleteHandler],
      MessageType.Incoming.IsCompleteRequest)
    initializeInputHandler(classOf[InputRequestReplyHandler],
      MessageType.Incoming.InputReply)
    initializeCommHandler(classOf[CommOpenHandler],
      MessageType.Incoming.CommOpen)
    initializeCommHandler(classOf[CommMsgHandler],
      MessageType.Incoming.CommMsg)
    initializeCommHandler(classOf[CommCloseHandler],
      MessageType.Incoming.CommClose)

    //  These are handlers for messages leaving the kernel through the sockets
    initializeSocketHandler(SocketType.Shell, MessageType.Outgoing.KernelInfoReply)
    initializeSocketHandler(SocketType.Shell, MessageType.Outgoing.CommInfoReply)
    initializeSocketHandler(SocketType.Shell, MessageType.Outgoing.ExecuteReply)
    initializeSocketHandler(SocketType.Shell, MessageType.Outgoing.CompleteReply)
    initializeSocketHandler(SocketType.Shell, MessageType.Outgoing.IsCompleteReply)

    initializeSocketHandler(SocketType.StdIn, MessageType.Outgoing.InputRequest)

    initializeSocketHandler(SocketType.IOPub, MessageType.Outgoing.ExecuteResult)
    initializeSocketHandler(SocketType.IOPub, MessageType.Outgoing.Stream)
    initializeSocketHandler(SocketType.IOPub, MessageType.Outgoing.DisplayData)
    initializeSocketHandler(SocketType.IOPub, MessageType.Outgoing.ClearOutput)
    initializeSocketHandler(SocketType.IOPub, MessageType.Outgoing.ExecuteInput)
    initializeSocketHandler(SocketType.IOPub, MessageType.Outgoing.Status)
    initializeSocketHandler(SocketType.IOPub, MessageType.Outgoing.Error)
    initializeSocketHandler(SocketType.IOPub, MessageType.Outgoing.CommOpen)
    initializeSocketHandler(SocketType.IOPub, MessageType.Outgoing.CommMsg)
    initializeSocketHandler(SocketType.IOPub, MessageType.Outgoing.CommClose)
  }
}
