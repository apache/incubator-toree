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
package com.ibm.spark.kernel.protocol.v5.comm

import com.ibm.spark.kernel.protocol.v5.UUID
import com.ibm.spark.kernel.protocol.v5.comm.CommCallbacks._

/**
 * Represents a point of communication to register new Comm entities (targets)
 * and attach handlers for various events. Uses external storage for the
 * Comm information.
 *
 * @param commStorage The storage used to save/load callbacks
 * @param defaultTargetName The default target name to use for functions
 */
class CommRegistrar(
  private val commStorage: CommStorage,
  private[comm] val defaultTargetName: Option[String] = None
) {

  /**
   * Registers a specific target for Comm communications.
   *
   * @param targetName The name of the target to register
   *
   * @return The current registrar (for chaining methods)
   */
  def register(targetName: String): CommRegistrar = {
    if (!commStorage.contains(targetName))
      commStorage(targetName) = new CommCallbacks() // Mark as registered

    // Return new instance with default target name specified for easier
    // method chaining
    new CommRegistrar(commStorage, Some(targetName))
  }

  /**
   * Links a target and a specific Comm Id together.
   *
   * @param targetName The name of the target to link
   * @param commId The Comm Id to link
   *
   * @return The current registrar (for chaining methods)
   */
  def link(targetName: String, commId: UUID): CommRegistrar = {
    // If target exists, copy contents to comm_id key
    if (commStorage.contains(targetName))
      commStorage(commId) = commStorage(targetName)
    // If comm_id exists, copy contents to target
    else if (commStorage.contains(commId))
      commStorage(targetName) = commStorage(commId)

    this
  }

  /**
   * Determines whether or not the Comm storage has the specified key.
   *
   * @param key The target name or Comm id
   *
   * @return True if it exists, otherwise false
   */
  def has(key: String): Boolean = commStorage.contains(key)

  /**
   * Retrieves the Comm callbacks associated with a specific key.
   *
   * @param key The target name or Comm id associated with the callbacks
   *
   * @return The Comm callbacks or None if no callbacks were found
   */
  def retrieve(key: String): Option[CommCallbacks] = commStorage.get(key)

  /**
   * Removes the specified target or id from the storage.
   *
   * @param key The target name or Comm Id to remove
   *
   * @return The current registrar (for chaining methods)
   */
  def remove(key: String): CommRegistrar = {
    commStorage.remove(key)

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
   * @return The current registrar (for chaining methods)
   */
  def addOpenHandler(func: OpenCallback) = {
    require(defaultTargetName.nonEmpty, "No default target name provided!")

    addOpenHandlerImpl(defaultTargetName.get)(func)
  }

  private def addOpenHandlerImpl(targetName: String)(func: OpenCallback) = {
    val commCallbacks =
      if (commStorage.contains(targetName)) commStorage(targetName)
      else new CommCallbacks()

    commStorage(targetName) = commCallbacks.addOpenCallback(func)

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
   * @return The current registrar (for chaining methods)
   */
  def addMsgHandler(func: MsgCallback) = {
    require(defaultTargetName.nonEmpty, "No default target name provided!")

    addMsgHandlerImpl(defaultTargetName.get)(func)
  }

  private def addMsgHandlerImpl(targetName: String)(func: MsgCallback) = {
    val commCallbacks =
      if (commStorage.contains(targetName)) commStorage(targetName)
      else new CommCallbacks()

    commStorage(targetName) = commCallbacks.addMsgCallback(func)

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
   * @return The current registrar (for chaining methods)
   */
  def addCloseHandler(func: CloseCallback) = {
    require(defaultTargetName.nonEmpty, "No default target name provided!")

    addCloseHandlerImpl(defaultTargetName.get)(func)
  }

  private def addCloseHandlerImpl(targetName: String)(func: CloseCallback) = {
    val commCallbacks =
      if (commStorage.contains(targetName)) commStorage(targetName)
      else new CommCallbacks()

    commStorage(targetName) = commCallbacks.addCloseCallback(func)

    this
  }
}
