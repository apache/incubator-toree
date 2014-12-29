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
package com.ibm.spark.comm

import com.ibm.spark.annotations.Experimental
import com.ibm.spark.comm.CommCallbacks._
import com.ibm.spark.kernel.protocol.v5

import scala.collection.immutable

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
   * @return The current registrar (for chaining methods)
   */
  def link(commId: v5.UUID): CommRegistrar = {
    require(defaultTargetName.nonEmpty, "No default target name provided!")

    linkImpl(defaultTargetName.get)(commId)
  }

  private def linkImpl(targetName: String)(commId: v5.UUID) = {
    val commIds = commStorage.getCommIdsFromTarget(targetName)
      .getOrElse(immutable.Vector.empty[v5.UUID])

    commStorage.setTargetCommIds(targetName, commIds :+ commId)

    this
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
   * @return The current registrar (for chaining methods)
   */
  def addOpenHandler(func: OpenCallback) = {
    require(defaultTargetName.nonEmpty, "No default target name provided!")

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
      commStorage.getTargetCallbacks(targetName).getOrElse(new CommCallbacks())

    commStorage.setTargetCallbacks(
      targetName, commCallbacks.addMsgCallback(func))

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
      commStorage.getTargetCallbacks(targetName).getOrElse(new CommCallbacks())

    commStorage.setTargetCallbacks(
      targetName, commCallbacks.addCloseCallback(func))

    this
  }
}
