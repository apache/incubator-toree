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

package org.apache.toree.communication.utils

import akka.actor.{Actor, Stash}
import org.apache.toree.utils.LogLike

/**
 * A trait to enforce ordered processing for messages of particular types.
 */
trait OrderedSupport extends Actor with Stash with LogLike {
  /**
   * Executes instead of the default receive when the Actor has begun
   * processing. Stashes incoming messages of particular types, defined by
   * [[orderedTypes]] function, for later processing. Uses
   * the default receive method for all other types. Upon receiving a
   * FinishedProcessing message, resumes processing all messages with the
   * default receive.
   * @return
   */
  def waiting : Receive = {
    case FinishedProcessing =>
      context.unbecome()
      unstashAll()
    case aVal: Any if (orderedTypes().contains(aVal.getClass)) =>
      logger.trace(s"Stashing message ${aVal} of type ${aVal.getClass}.")
      stash()
    case aVal: Any =>
      logger.trace(s"Forwarding message ${aVal} of type ${aVal.getClass} " +
        "to default receive.")
      receive(aVal)
  }

  /**
   * Suspends the default receive method for types defined by the
   * [[orderedTypes]] function.
   */
  def startProcessing(): Unit = {
    logger.debug("Actor is in processing state and will stash messages of " +
      s"types: ${orderedTypes.mkString(" ")}")
    context.become(waiting, discardOld = false)
  }

  /**
   * Resumes the default receive method for all message types.
   */
  def finishedProcessing(): Unit = {
    logger.debug("Actor is no longer in processing state.")
    self ! FinishedProcessing
  }

  /**
   * Executes a block of code, wrapping it in start/finished processing
   * needed for ordered execution.
   *
   * @param block The block to execute
   * @tparam T The return type of the block
   * @return The result of executing the block
   */
  def withProcessing[T](block: => T): T = {
    startProcessing()
    val results = block
    finishedProcessing()
    results
  }

  /**
   * Defines the types that will be stashed by [[waiting]]
   * while the Actor is in processing state.
   * @return
   */
  def orderedTypes(): Seq[Class[_]]

  case object FinishedProcessing
}
