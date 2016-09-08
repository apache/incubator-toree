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

package org.apache.toree.comm

import org.apache.toree.annotations.Experimental
import org.apache.toree.comm.CommCallbacks._
import org.apache.toree.kernel.protocol.v5

import scala.annotation.tailrec
import scala.collection.immutable
import java.lang.AssertionError

/**
 * Represents a point of communication to register new Comm entities (targets)
 * and attach handlers for various events. Uses external storage for the
 * Comm information.
 *
 * @param commStorage The storage used to save/load callbacks
 * @param defaultTargetName The default target name to use for functions
 */
@Experimental
class CommRegistrar(
  private val commStorage: CommStorage,
  private[comm] val defaultTargetName: Option[String] = None
) {

  /**
   * Returns an updated copy of the registrar that is using the specified
   * target name as the default for chaining.
   *
   * @param targetName The name of the target to treat as the default
   *
   * @return The updated registrar (for chaining methods)
   */
  def withTarget(targetName: String): CommRegistrar = {
    new CommRegistrar(commStorage, Some(targetName))
  }

  /**
   * Registers a specific target for Comm communications.
   *
   * @param targetName The name of the target to register
   *
   * @return The current registrar (for chaining methods)
   */
  def register(targetName: String): CommRegistrar = {
    // Mark as registered if not already
    if (!commStorage.hasTargetCallbacks(targetName))
      commStorage.setTargetCallbacks(targetName, new CommCallbacks())

    // Return new instance with default target name specified for easier
    // method chaining
    new CommRegistrar(commStorage, Some(targetName))
  }

  /**
   * Unregisters a specific target for Comm communications.
   *
   * @param targetName The name of the target to unregister
   *
   * @return Some collection of callbacks associated with the target if it was
   *         registered, otherwise None
   */
  def unregister(targetName: String): Option[CommCallbacks] = {
    commStorage.removeTargetCallbacks(targetName)
  }

  /**
   * Indicates whether or not the specified target is currently registered
   * with this registrar.
   *
   * @param targetName The name of the target
   *
   * @return True if the target is registered, otherwise false
   */
  def isRegistered(targetName: String): Boolean = {
    commStorage.hasTargetCallbacks(targetName)
  }

  /**
   * Links a target and a specific Comm id together.
   *
   * @param targetName The name of the target to link
   * @param commId The Comm Id to link
   *
   * @return The current registrar (for chaining methods)
   */
  def link(targetName: String, commId: v5.UUID): CommRegistrar =
    linkImpl(targetName)(commId)

  /**
   * Links a target and a specific Comm id together.
   *
   * @param commId The Comm Id to link
   *
   * @throws AssertionError When not chaining off of a register call
   *
   * @return The current registrar (for chaining methods)
   */
  def link(commId: v5.UUID): CommRegistrar = {
    assert(defaultTargetName.nonEmpty, "No default target name provided!")

    linkImpl(defaultTargetName.get)(commId)
  }

  private def linkImpl(targetName: String)(commId: v5.UUID) = {
    val commIds = commStorage.getCommIdsFromTarget(targetName)
      .getOrElse(immutable.Vector.empty[v5.UUID])

    commStorage.setTargetCommIds(targetName, commIds :+ commId)

    this
  }

  /**
   * Retrieves the current links for the specified target.
   *
   * @param targetName The name of the target whose links to retrieve
   *
   * @return The collection of link ids
   */
  def getLinks(targetName: String): Seq[v5.UUID] =
    getLinksImpl(targetName)

  /**
   * Retrieves the target associated with the specified link.
   *
   * @param commId The Comm id whose target to look up
   *
   * @return Some target name if found, otherwise None
   */
  def getTargetFromLink(commId: v5.UUID): Option[String] = {
    commStorage.getTargetFromCommId(commId)
  }

  /**
   * Retrieves the current links for the current target.
   *
   * @throws AssertionError When not chaining off of a register call
   *
   * @return The collection of link ids
   */
  def getLinks: Seq[v5.UUID] = {
    assert(defaultTargetName.nonEmpty, "No default target name provided!")

    getLinksImpl(defaultTargetName.get)
  }

  private def getLinksImpl(targetName: String): Seq[v5.UUID] = {
    commStorage
      .getCommIdsFromTarget(targetName)
      .getOrElse(Nil)
  }

  /**
   * Unlinks a target and a specific Comm id based on the provided id.
   *
   * @param commId The id of the Comm instance to unlink from its target
   *
   * @return The current registrar (for chaining methods)
   */
  def unlink(commId: v5.UUID): CommRegistrar = {
    commStorage.removeCommIdFromTarget(commId)

    this
  }

  /**
   * Adds an entry to the list of open Comm handlers.
   *
   * @param targetName The name of the target whose open event to monitor
   * @param func The handler function to trigger when an open is received
   *
   * @return The current registrar (for chaining methods)
   */
  def addOpenHandler(targetName: String, func: OpenCallback) =
    addOpenHandlerImpl(targetName)(func)

  /**
   * Adds an entry to the list of open Comm handlers.
   *
   * @param func The handler function to trigger when an open is received
   *
   * @throws AssertionError When not chaining off of a register call
   *
   * @return The current registrar (for chaining methods)
   */
  def addOpenHandler(func: OpenCallback) = {
    assert(defaultTargetName.nonEmpty, "No default target name provided!")

    addOpenHandlerImpl(defaultTargetName.get)(func)
  }

  private def addOpenHandlerImpl(targetName: String)(func: OpenCallback) = {
    val commCallbacks =
      commStorage.getTargetCallbacks(targetName).getOrElse(new CommCallbacks())

    commStorage.setTargetCallbacks(
      targetName, commCallbacks.addOpenCallback(func))

    this
  }

  /**
   * Removes the specified callback from the list of open Comm handlers.
   *
   * @param targetName The name of the target to remove the open callback
   * @param func The callback to remove
   *
   * @return The current registrar (for chaining methods)
   */
  def removeOpenHandler(targetName: String, func: OpenCallback) =
    removeOpenHandlerImpl(targetName)(func)

  /**
   * Removes the specified callback from the list of open Comm handlers.
   *
   * @param func The callback to remove
   *
   * @throws AssertionError When not chaining off of a register call
   *
   * @return The current registrar (for chaining methods)
   */
  def removeOpenHandler(func: OpenCallback) = {
    assert(defaultTargetName.nonEmpty, "No default target name provided!")

    removeOpenHandlerImpl(defaultTargetName.get)(func)
  }

  private def removeOpenHandlerImpl(targetName: String)(func: OpenCallback) = {
    val commCallbacks =
      commStorage.getTargetCallbacks(targetName).getOrElse(new CommCallbacks())

    commStorage.setTargetCallbacks(
      targetName, commCallbacks.removeOpenCallback(func))

    this
  }

  /**
   * Adds an entry to the list of msg Comm handlers.
   *
   * @param targetName The name of the target whose msg event to monitor
   * @param func The handler function to trigger when a msg is received
   *
   * @return The current registrar (for chaining methods)
   */
  def addMsgHandler(targetName: String, func: MsgCallback) =
    addMsgHandlerImpl(targetName)(func)

  /**
   * Adds an entry to the list of msg Comm handlers.
   *
   * @param func The handler function to trigger when a msg is received
   *
   * @throws AssertionError When not chaining off of a register call
   *
   * @return The current registrar (for chaining methods)
   */
  def addMsgHandler(func: MsgCallback) = {
    assert(defaultTargetName.nonEmpty, "No default target name provided!")

    addMsgHandlerImpl(defaultTargetName.get)(func)
  }

  private def addMsgHandlerImpl(targetName: String)(func: MsgCallback) = {
    val commCallbacks =
      commStorage.getTargetCallbacks(targetName).getOrElse(new CommCallbacks())

    commStorage.setTargetCallbacks(
      targetName, commCallbacks.addMsgCallback(func))

    this
  }

  /**
   * Removes the specified callback from the list of msg Comm handlers.
   *
   * @param targetName The name of the target to remove the msg callback
   * @param func The callback to remove
   *
   * @return The current registrar (for chaining methods)
   */
  def removeMsgHandler(targetName: String, func: MsgCallback) =
    removeMsgHandlerImpl(targetName)(func)

  /**
   * Removes the specified callback from the list of msg Comm handlers.
   *
   * @param func The callback to remove
   *
   * @throws AssertionError When not chaining off of a register call
   *
   * @return The current registrar (for chaining methods)
   */
  def removeMsgHandler(func: MsgCallback) = {
    assert(defaultTargetName.nonEmpty, "No default target name provided!")

    removeMsgHandlerImpl(defaultTargetName.get)(func)
  }

  private def removeMsgHandlerImpl(targetName: String)(func: MsgCallback) = {
    val commCallbacks =
      commStorage.getTargetCallbacks(targetName).getOrElse(new CommCallbacks())

    commStorage.setTargetCallbacks(
      targetName, commCallbacks.removeMsgCallback(func))

    this
  }

  /**
   * Adds an entry to the list of close Comm handlers.
   *
   * @param targetName The name of the target whose close event to monitor
   * @param func The handler function to trigger when a close is received
   *
   * @return The current registrar (for chaining methods)
   */
  def addCloseHandler(targetName: String, func: CloseCallback) =
    addCloseHandlerImpl(targetName)(func)

  /**
   * Adds an entry to the list of close Comm handlers.
   *
   * @param func The handler function to trigger when a close is received
   *
   * @throws AssertionError When not chaining off of a register call
   *
   * @return The current registrar (for chaining methods)
   */
  def addCloseHandler(func: CloseCallback) = {
    assert(defaultTargetName.nonEmpty, "No default target name provided!")

    addCloseHandlerImpl(defaultTargetName.get)(func)
  }

  private def addCloseHandlerImpl(targetName: String)(func: CloseCallback) = {
    val commCallbacks =
      commStorage.getTargetCallbacks(targetName).getOrElse(new CommCallbacks())

    commStorage.setTargetCallbacks(
      targetName, commCallbacks.addCloseCallback(func))

    this
  }

  /**
   * Removes the specified callback from the list of close Comm handlers.
   *
   * @param targetName The name of the target to remove the close callback
   * @param func The callback to remove
   *
   * @return The current registrar (for chaining methods)
   */
  def removeCloseHandler(targetName: String, func: CloseCallback) =
    removeCloseHandlerImpl(targetName)(func)

  /**
   * Removes the specified callback from the list of close Comm handlers.
   *
   * @param func The callback to remove
   *
   * @throws AssertionError When not chaining off of a register call
   *
   * @return The current registrar (for chaining methods)
   */
  def removeCloseHandler(func: CloseCallback) = {
    assert(defaultTargetName.nonEmpty, "No default target name provided!")

    removeCloseHandlerImpl(defaultTargetName.get)(func)
  }

  private def removeCloseHandlerImpl(targetName: String)(
    func: CloseCallback
  ) = {
    val commCallbacks =
      commStorage.getTargetCallbacks(targetName).getOrElse(new CommCallbacks())

    commStorage.setTargetCallbacks(
      targetName, commCallbacks.removeCloseCallback(func))

    this
  }

  /**
   * Retrieves all open callbacks for the target.
   *
   * @param targetName The name of the target whose open callbacks to retrieve
   *
   * @return The collection of open callbacks
   */
  def getOpenHandlers(targetName: String): Seq[OpenCallback] =
    getOpenHandlersImpl(targetName)

  /**
   * Retrieves all open callbacks for the current target.
   *
   * @throws AssertionError When not chaining off of a register call
   *
   * @return The collection of open callbacks
   */
  def getOpenHandlers: Seq[OpenCallback] = {
    assert(defaultTargetName.nonEmpty, "No default target name provided!")

    getOpenHandlersImpl(defaultTargetName.get)
  }

  private def getOpenHandlersImpl(targetName: String): Seq[OpenCallback] = {
    commStorage
      .getTargetCallbacks(targetName)
      .map(_.openCallbacks)
      .getOrElse(Nil)
  }

  /**
   * Retrieves all msg callbacks for the target.
   *
   * @param targetName The name of the target whose msg callbacks to retrieve
   *
   * @return The collection of msg callbacks
   */
  def getMsgHandlers(targetName: String): Seq[MsgCallback] =
    getMsgHandlersImpl(targetName)

  /**
   * Retrieves all msg callbacks for the current target.
   *
   * @throws AssertionError When not chaining off of a register call
   *
   * @return The collection of msg callbacks
   */
  def getMsgHandlers: Seq[MsgCallback] = {
    assert(defaultTargetName.nonEmpty, "No default target name provided!")

    getMsgHandlersImpl(defaultTargetName.get)
  }

  private def getMsgHandlersImpl(targetName: String): Seq[MsgCallback] = {
    commStorage
      .getTargetCallbacks(targetName)
      .map(_.msgCallbacks)
      .getOrElse(Nil)
  }

  /**
   * Retrieves all close callbacks for the target.
   *
   * @param targetName The name of the target whose close callbacks to retrieve
   *
   * @return The collection of close callbacks
   */
  def getCloseHandlers(targetName: String): Seq[CloseCallback] =
    getCloseHandlersImpl(targetName)

  /**
   * Retrieves all close callbacks for the current target.
   *
   * @throws AssertionError When not chaining off of a register call
   *
   * @return The collection of close callbacks
   */
  def getCloseHandlers: Seq[CloseCallback] = {
    assert(defaultTargetName.nonEmpty, "No default target name provided!")

    getCloseHandlersImpl(defaultTargetName.get)
  }

  private def getCloseHandlersImpl(targetName: String): Seq[CloseCallback] = {
    commStorage
      .getTargetCallbacks(targetName)
      .map(_.closeCallbacks)
      .getOrElse(Nil)
  }
}
